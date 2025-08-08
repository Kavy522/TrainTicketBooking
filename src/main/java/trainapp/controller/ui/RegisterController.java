package trainapp.controller.ui;

import javafx.event.ActionEvent;
import org.mindrot.jbcrypt.BCrypt;
import trainapp.util.SceneManager;

public class RegisterController {

    public void register(){
    }

    public void handleLogin(ActionEvent event) {
        SceneManager.switchScene("/fxml/Login.fxml");
    }
}
