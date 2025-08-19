package trainapp.controller.ui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import trainapp.dao.UserDAO;
import trainapp.model.User;
import trainapp.util.SceneManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * UserManagementController manages comprehensive user administration and account oversight.
 * Provides complete user lifecycle management with advanced filtering and analytics capabilities.
 *
 * <p>Key Features:
 * <ul>
 *   <li>Complete user account management with CRUD operations</li>
 *   <li>Advanced filtering by status and comprehensive search functionality</li>
 *   <li>Real-time user statistics with status-based analytics</li>
 *   <li>Detailed user information display with activity tracking</li>
 *   <li>Secure user creation and modification with validation</li>
 *   <li>Responsive table layout with contextual action buttons</li>
 * </ul>
 *
 * <p>Administrative Operations:
 * <ul>
 *   <li>User account creation with comprehensive validation</li>
 *   <li>Account modification with optional password updates</li>
 *   <li>Safe account deletion with confirmation dialogs</li>
 *   <li>Detailed user profile viewing with complete information</li>
 *   <li>Status management with visual indicators</li>
 * </ul>
 *
 * <p>Analytics and Monitoring Features:
 * <ul>
 *   <li>Real-time user statistics (total, active, new, inactive)</li>
 *   <li>Status-based user categorization and filtering</li>
 *   <li>Account activity tracking with last login information</li>
 *   <li>Search functionality across user attributes</li>
 *   <li>Data refresh capabilities for real-time monitoring</li>
 * </ul>
 */
public class UserManagementController {

    // -------------------------------------------------------------------------
    // FXML UI Components
    // -------------------------------------------------------------------------

    // Search and Filter Controls
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilterCombo;

    // Statistics Display
    @FXML private Label totalUsersLabel;
    @FXML private Label activeUsersLabel;
    @FXML private Label newUsersLabel;
    @FXML private Label inactiveUsersLabel;

    // Data Table and Columns
    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, Number> colId;
    @FXML private TableColumn<User, String> colName;
    @FXML private TableColumn<User, String> colEmail;
    @FXML private TableColumn<User, String> colPhone;
    @FXML private TableColumn<User, String> colCreatedAt;
    @FXML private TableColumn<User, String> colLastLogin;
    @FXML private TableColumn<User, String> colStatus;
    @FXML private TableColumn<User, Void> colActions;

    // Status and Messaging
    @FXML private Label messageLabel;

    // -------------------------------------------------------------------------
    // Data Access and State Management
    // -------------------------------------------------------------------------

    // Services and DAOs
    private final UserDAO userDAO = new UserDAO();

    // Data Collections
    private ObservableList<User> allUsers = FXCollections.observableArrayList();
    private ObservableList<User> filteredUsers = FXCollections.observableArrayList();

    // -------------------------------------------------------------------------
    // Initialization and Setup
    // -------------------------------------------------------------------------

    /**
     * Initializes the user management interface with full-screen layout and data loading.
     * Called automatically by JavaFX after FXML loading.
     */
    @FXML
    public void initialize() {
        Platform.runLater(() -> {
            Stage stage = (Stage) userTable.getScene().getWindow();
            if (stage != null) {
                stage.setMaximized(true);
            }
        });

        setupTableColumns();
        setupFilters();
        loadUsers();
        updateStatistics();
    }

    /**
     * Sets up comprehensive table columns with data binding and custom formatting.
     */
    private void setupTableColumns() {
        // Configure table columns
        colId.setCellValueFactory(cellData -> cellData.getValue().userIdProperty());
        colName.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        colEmail.setCellValueFactory(cellData -> cellData.getValue().emailProperty());
        colPhone.setCellValueFactory(cellData -> cellData.getValue().phoneProperty());

        // Created At column
        colCreatedAt.setCellValueFactory(cellData -> {
            String formattedDate = cellData.getValue().getCreatedAtFormatted();
            return new javafx.beans.property.SimpleStringProperty(formattedDate);
        });

        // Last Login column
        colLastLogin.setCellValueFactory(cellData -> {
            String formattedDate = cellData.getValue().getLastLoginFormatted();
            return new javafx.beans.property.SimpleStringProperty(formattedDate);
        });

        // Status column with custom styling
        colStatus.setCellValueFactory(cellData -> {
            String status = cellData.getValue().getStatusDisplay();
            return new javafx.beans.property.SimpleStringProperty(status);
        });

        colStatus.setCellFactory(column -> new TableCell<User, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label statusLabel = new Label(status);
                    statusLabel.getStyleClass().add("status-" + status.toLowerCase());
                    setGraphic(statusLabel);
                    setText(null);
                }
            }
        });

        // Actions column
        colActions.setCellFactory(column -> new TableCell<User, Void>() {
            private final Button viewBtn = new Button("View");
            private final Button editBtn = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");
            private final HBox actionBox = new HBox(5);

            {
                viewBtn.getStyleClass().add("action-btn-view");
                editBtn.getStyleClass().add("action-btn-edit");
                deleteBtn.getStyleClass().add("action-btn-delete");

                actionBox.setAlignment(Pos.CENTER);
                actionBox.getChildren().addAll(viewBtn, editBtn, deleteBtn);

                viewBtn.setOnAction(e -> {
                    User user = getTableView().getItems().get(getIndex());
                    viewUserDetails(user);
                });

                editBtn.setOnAction(e -> {
                    User user = getTableView().getItems().get(getIndex());
                    handleEditUser(user);
                });

                deleteBtn.setOnAction(e -> {
                    User user = getTableView().getItems().get(getIndex());
                    handleDeleteUser(user);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(actionBox);
                }
            }
        });

        userTable.setItems(filteredUsers);
    }

    /**
     * Sets up filter controls with status options and event handlers.
     */
    private void setupFilters() {
        // Status filter
        statusFilterCombo.getItems().addAll("All Status", "Active", "Inactive", "New");
        statusFilterCombo.setValue("All Status");

        // Add listener
        statusFilterCombo.setOnAction(e -> applyFilters());
    }

    // -------------------------------------------------------------------------
    // Data Loading and Management
    // -------------------------------------------------------------------------

    /**
     * Loads all users from database and updates display.
     */
    private void loadUsers() {
        try {
            List<User> users = userDAO.getAllUsers();
            allUsers.setAll(users);
            filteredUsers.setAll(users);

            if (users.isEmpty()) {
                showMessage("No users found in the database.", "info");
            } else {
                showMessage("Loaded " + users.size() + " users successfully.", "success");
            }
        } catch (Exception e) {
            showMessage("Error loading users: " + e.getMessage(), "error");
            e.printStackTrace();
        }
    }

    /**
     * Updates user statistics with current data calculations.
     */
    private void updateStatistics() {
        try {
            int totalUsers = allUsers.size();
            long activeUsers = allUsers.stream()
                    .filter(user -> "Active".equals(user.getStatusDisplay()))
                    .count();
            long newUsers = allUsers.stream()
                    .filter(user -> "New".equals(user.getStatusDisplay()))
                    .count();
            long inactiveUsers = allUsers.stream()
                    .filter(user -> "Inactive".equals(user.getStatusDisplay()))
                    .count();

            Platform.runLater(() -> {
                totalUsersLabel.setText(String.valueOf(totalUsers));
                activeUsersLabel.setText(String.valueOf(activeUsers));
                newUsersLabel.setText(String.valueOf(newUsers));
                inactiveUsersLabel.setText(String.valueOf(inactiveUsers));
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // -------------------------------------------------------------------------
    // Search and Filter Operations
    // -------------------------------------------------------------------------

    /**
     * Handles search functionality with current filter criteria.
     */
    @FXML
    public void handleSearch() {
        applyFilters();
    }

    /**
     * Clears all search and filter criteria and resets display.
     */
    @FXML
    public void handleClearSearch() {
        searchField.clear();
        statusFilterCombo.setValue("All Status");
        filteredUsers.setAll(allUsers);
        showMessage("Filters cleared.", "info");
    }

    /**
     * Applies comprehensive filtering based on search text and status selection.
     */
    private void applyFilters() {
        String searchText = searchField.getText().trim().toLowerCase();
        String selectedStatus = statusFilterCombo.getValue();

        List<User> filtered = allUsers.stream()
                .filter(user -> {
                    // Search filter
                    boolean matchesSearch = searchText.isEmpty() ||
                            user.getName().toLowerCase().contains(searchText) ||
                            user.getEmail().toLowerCase().contains(searchText) ||
                            user.getPhone().toLowerCase().contains(searchText);

                    // Status filter
                    boolean matchesStatus = "All Status".equals(selectedStatus) ||
                            user.getStatusDisplay().equals(selectedStatus);

                    return matchesSearch && matchesStatus;
                })
                .collect(Collectors.toList());

        filteredUsers.setAll(filtered);
        showMessage("Found " + filtered.size() + " users matching criteria.", "info");
    }

    // -------------------------------------------------------------------------
    // User CRUD Operations
    // -------------------------------------------------------------------------

    /**
     * Handles adding a new user.
     */
    @FXML
    public void handleAddUser() {
        showUserDialog(null);
    }

    /**
     * Handles editing an existing user.
     *
     * @param user the user to edit
     */
    private void handleEditUser(User user) {
        showUserDialog(user);
    }

    /**
     * Handles deleting a user with confirmation.
     *
     * @param user the user to delete
     */
    private void handleDeleteUser(User user) {
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Delete User");
        confirmDialog.setHeaderText("Delete User: " + user.getName());
        confirmDialog.setContentText("Are you sure you want to delete this user? This action cannot be undone.");

        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                if (userDAO.deleteUser(user.getUserId())) {
                    allUsers.remove(user);
                    filteredUsers.remove(user);
                    updateStatistics();
                    showMessage("User deleted successfully", "success");
                } else {
                    showMessage("Failed to delete user", "error");
                }
            } catch (Exception e) {
                showMessage("Error deleting user: " + e.getMessage(), "error");
            }
        }
    }

    /**
     * Displays detailed user information in dialog.
     *
     * @param user the user to view details for
     */
    private void viewUserDetails(User user) {
        try {
            Alert detailsDialog = new Alert(Alert.AlertType.INFORMATION);
            detailsDialog.setTitle("User Details");
            detailsDialog.setHeaderText("User: " + user.getName());

            StringBuilder details = new StringBuilder();
            details.append("User ID: ").append(user.getUserId()).append("\n");
            details.append("Name: ").append(user.getName()).append("\n");
            details.append("Email: ").append(user.getEmail()).append("\n");
            details.append("Phone: ").append(user.getPhone()).append("\n");
            details.append("Status: ").append(user.getStatusDisplay()).append("\n");
            details.append("Created: ").append(user.getCreatedAtFormatted()).append("\n");
            details.append("Last Login: ").append(user.getLastLoginFormatted());

            detailsDialog.setContentText(details.toString());
            detailsDialog.getDialogPane().setPrefWidth(400);
            detailsDialog.showAndWait();

        } catch (Exception e) {
            showMessage("Error loading user details: " + e.getMessage(), "error");
        }
    }

    // -------------------------------------------------------------------------
    // User Dialog Management
    // -------------------------------------------------------------------------

    /**
     * Shows user add/edit dialog with comprehensive form validation.
     *
     * @param user the user to edit, or null for new user
     */
    private void showUserDialog(User user) {
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle(user == null ? "Add New User" : "Edit User");
        dialog.setHeaderText(user == null ? "Enter user details" : "Update user details");

        // Create form fields
        TextField nameField = new TextField();
        TextField emailField = new TextField();
        TextField phoneField = new TextField();
        PasswordField passwordField = new PasswordField();

        // Pre-populate if editing
        if (user != null) {
            nameField.setText(user.getName());
            emailField.setText(user.getEmail());
            phoneField.setText(user.getPhone());
            passwordField.setPromptText("Leave blank to keep current password");
        }

        // Create form layout
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Email:"), 0, 1);
        grid.add(emailField, 1, 1);
        grid.add(new Label("Phone:"), 0, 2);
        grid.add(phoneField, 1, 2);
        grid.add(new Label("Password:"), 0, 3);
        grid.add(passwordField, 1, 3);

        dialog.getDialogPane().setContent(grid);

        // Add buttons
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Enable/disable save button based on input validation
        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.setDisable(true);

        // Validation
        Runnable validateInput = () -> {
            boolean valid = !nameField.getText().trim().isEmpty() &&
                    !emailField.getText().trim().isEmpty() &&
                    !phoneField.getText().trim().isEmpty();
            if (user == null) {
                valid = valid && !passwordField.getText().trim().isEmpty();
            }
            saveButton.setDisable(!valid);
        };

        nameField.textProperty().addListener((obs, oldVal, newVal) -> validateInput.run());
        emailField.textProperty().addListener((obs, oldVal, newVal) -> validateInput.run());
        phoneField.textProperty().addListener((obs, oldVal, newVal) -> validateInput.run());
        passwordField.textProperty().addListener((obs, oldVal, newVal) -> validateInput.run());

        // Result converter
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                User newUser = user != null ? user : new User();
                newUser.setName(nameField.getText().trim());
                newUser.setEmail(emailField.getText().trim());
                newUser.setPhone(phoneField.getText().trim());

                if (!passwordField.getText().trim().isEmpty()) {
                    newUser.setPasswordHash(passwordField.getText().trim()); // In real app, hash this
                }

                if (user == null) {
                    newUser.setCreatedAt(LocalDateTime.now());
                }

                return newUser;
            }
            return null;
        });

        Optional<User> result = dialog.showAndWait();
        result.ifPresent(this::saveUser);
    }

    /**
     * Saves user to database with appropriate add or update operation.
     *
     * @param user the user to save
     */
    private void saveUser(User user) {
        try {
            boolean success;
            if (user.getUserId() == 0) {
                // Adding new user
                success = userDAO.addUser(user);
                if (success) {
                    showMessage("User added successfully", "success");
                }
            } else {
                // Updating existing user
                success = userDAO.updateUser(user);
                if (success) {
                    showMessage("User updated successfully", "success");
                }
            }

            if (success) {
                loadUsers();
                updateStatistics();
            } else {
                showMessage("Failed to save user", "error");
            }
        } catch (Exception e) {
            showMessage("Error saving user: " + e.getMessage(), "error");
            e.printStackTrace();
        }
    }

    // -------------------------------------------------------------------------
    // Data Refresh and Window Management
    // -------------------------------------------------------------------------

    /**
     * Handles data refresh operation.
     */
    @FXML
    public void handleRefresh() {
        loadUsers();
        updateStatistics();
        showMessage("Data refreshed successfully.", "success");
    }

    /**
     * Closes the user management window.
     */
    @FXML
    public void handleClose() {
        SceneManager.switchScene("/fxml/AdminProfile.fxml");
    }

    // -------------------------------------------------------------------------
    // UI State Management and Messaging
    // -------------------------------------------------------------------------

    /**
     * Displays status message with automatic hiding after delay.
     *
     * @param message the message to display
     * @param type the message type for styling
     */
    private void showMessage(String message, String type) {
        messageLabel.setText(message);
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);

        // Clear existing style classes
        messageLabel.getStyleClass().removeAll("success", "error", "info");
        messageLabel.getStyleClass().add(type);

        // Auto-hide message after 3 seconds
        new Thread(() -> {
            try {
                Thread.sleep(3000);
                Platform.runLater(() -> {
                    messageLabel.setVisible(false);
                    messageLabel.setManaged(false);
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
}