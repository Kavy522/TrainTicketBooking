package trainapp.service;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeBodyPart;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EmailService {

    // Email Configuration (Consider moving to properties file)
    private final String smtpHost = "smtp.gmail.com";
    private final String smtpPort = "587";
    private final String emailUsername = "thakarkavy522@gmail.com"; // Configure this
    private final String emailPassword = "vsxowhzonaeageas"; // Use app password
    private final String fromName = "Tailyatri";

    // Thread pool for async email sending
    private final ExecutorService emailExecutor = Executors.newFixedThreadPool(3);

    /**
     * Send OTP email (Main method for password reset)
     */
    public boolean sendOtpEmail(String toEmail, String otpCode) {
        try {
            Session session = createEmailSession();
            Message message = createOtpMessage(session, toEmail, otpCode);
            Transport.send(message);

            System.out.println("OTP email sent successfully to: " + toEmail);
            return true;

        } catch (Exception e) {
            System.err.println("Error sending OTP email to " + toEmail + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Send OTP email asynchronously
     */
    public CompletableFuture<Boolean> sendOtpEmailAsync(String toEmail, String otpCode) {
        return CompletableFuture.supplyAsync(() -> sendOtpEmail(toEmail, otpCode), emailExecutor);
    }

    /**
     * Send booking confirmation email with ticket and invoice attachments
     */
    public boolean sendBookingConfirmationWithAttachments(String toEmail, String userName, String pnr,
                                                          String trainDetails, String journeyDetails,
                                                          byte[] ticketPdf, byte[] invoicePdf) {
        try {
            System.out.println("Sending booking confirmation with attachments to: " + toEmail);

            Session session = createEmailSession();
            Message message = new MimeMessage(session);

            // Set sender
            setFromAddressSafely(message);

            // Set recipient
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("🎫 Booking Confirmed - PNR: " + pnr + " | Tailyatri");

            // Create multipart message
            Multipart multipart = new MimeMultipart();

            // Add HTML body
            MimeBodyPart htmlPart = new MimeBodyPart();
            String emailContent = buildBookingConfirmationWithAttachmentsContent(userName, pnr, trainDetails, journeyDetails);
            htmlPart.setContent(emailContent, "text/html; charset=utf-8");
            multipart.addBodyPart(htmlPart);

            // Add ticket PDF attachment if provided
            if (ticketPdf != null && ticketPdf.length > 0) {
                MimeBodyPart ticketAttachment = new MimeBodyPart();
                ticketAttachment.setDataHandler(new DataHandler(new ByteArrayDataSource(ticketPdf, "application/pdf")));
                ticketAttachment.setFileName("E-Ticket_" + pnr + ".pdf");
                ticketAttachment.setHeader("Content-ID", "<ticket>");
                multipart.addBodyPart(ticketAttachment);
            }

            // Add invoice PDF attachment if provided
            if (invoicePdf != null && invoicePdf.length > 0) {
                MimeBodyPart invoiceAttachment = new MimeBodyPart();
                invoiceAttachment.setDataHandler(new DataHandler(new ByteArrayDataSource(invoicePdf, "application/pdf")));
                invoiceAttachment.setFileName("Invoice_" + pnr + ".pdf");
                invoiceAttachment.setHeader("Content-ID", "<invoice>");
                multipart.addBodyPart(invoiceAttachment);
            }

            // Set the content
            message.setContent(multipart);

            // Send email
            Transport.send(message);

            System.out.println("Booking confirmation email with attachments sent successfully to: " + toEmail);
            return true;

        } catch (Exception e) {
            System.err.println("Error sending booking confirmation email with attachments: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Send booking confirmation email with attachments asynchronously
     */
    public CompletableFuture<Boolean> sendBookingConfirmationWithAttachmentsAsync(String toEmail, String userName,
                                                                                  String pnr, String trainDetails,
                                                                                  String journeyDetails, byte[] ticketPdf,
                                                                                  byte[] invoicePdf) {
        return CompletableFuture.supplyAsync(() ->
                        sendBookingConfirmationWithAttachments(toEmail, userName, pnr, trainDetails, journeyDetails, ticketPdf, invoicePdf),
                emailExecutor);
    }

    /**
     * Send generic email with single attachment
     */
    public boolean sendEmailWithAttachment(String toEmail, String subject, String messageBody,
                                           byte[] attachment, String attachmentName) {
        try {
            System.out.println("Sending email with attachment to: " + toEmail);

            Session session = createEmailSession();
            Message message = new MimeMessage(session);

            setFromAddressSafely(message);
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject(subject);

            // Create multipart message
            Multipart multipart = new MimeMultipart();

            // Add text part
            MimeBodyPart textPart = new MimeBodyPart();
            textPart.setContent(messageBody, "text/html; charset=utf-8");
            multipart.addBodyPart(textPart);

            // Add attachment if provided
            if (attachment != null && attachment.length > 0) {
                MimeBodyPart attachmentPart = new MimeBodyPart();
                attachmentPart.setDataHandler(new DataHandler(new ByteArrayDataSource(attachment, "application/pdf")));
                attachmentPart.setDataHandler(new DataHandler(new ByteArrayDataSource(attachment, "application/pdf")));
                attachmentPart.setFileName(attachmentName);
                multipart.addBodyPart(attachmentPart);
            }

            message.setContent(multipart);
            Transport.send(message);

            System.out.println("Email with attachment sent successfully to: " + toEmail);
            return true;

        } catch (Exception e) {
            System.err.println("Error sending email with attachment: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Send simple email without attachments
     */
    public boolean sendEmail(String toEmail, String subject, String messageBody) {
        try {
            System.out.println("Sending simple email to: " + toEmail);

            Session session = createEmailSession();
            Message message = new MimeMessage(session);

            setFromAddressSafely(message);
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject(subject);
            message.setContent(messageBody, "text/html; charset=utf-8");

            Transport.send(message);

            System.out.println("Simple email sent successfully to: " + toEmail);
            return true;

        } catch (Exception e) {
            System.err.println("Error sending simple email: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Send password reset success notification email
     */
    public boolean sendPasswordResetSuccessEmail(String toEmail, String userName) {
        try {
            Session session = createEmailSession();
            Message message = createPasswordResetSuccessMessage(session, toEmail, userName);
            Transport.send(message);

            System.out.println("Password reset success email sent to: " + toEmail);
            return true;

        } catch (Exception e) {
            System.err.println("Error sending password reset success email: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Send welcome email for new registrations
     */
    public boolean sendWelcomeEmail(String toEmail, String userName) {
        try {
            Session session = createEmailSession();
            Message message = createWelcomeMessage(session, toEmail, userName);
            Transport.send(message);

            System.out.println("Welcome email sent to: " + toEmail);
            return true;

        } catch (Exception e) {
            System.err.println("Error sending welcome email: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Send booking confirmation email (legacy method - kept for compatibility)
     */
    public boolean sendBookingConfirmationEmail(String toEmail, String userName, String bookingDetails) {
        try {
            Session session = createEmailSession();
            Message message = createBookingConfirmationMessage(session, toEmail, userName, bookingDetails);
            Transport.send(message);

            System.out.println("Booking confirmation email sent to: " + toEmail);
            return true;

        } catch (Exception e) {
            System.err.println("Error sending booking confirmation email: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Create email session with authentication
     */
    private Session createEmailSession() {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.port", smtpPort);
        props.put("mail.smtp.ssl.trust", smtpHost);
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");

        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(emailUsername, emailPassword);
            }
        });
    }

    /**
     * Helper method to set from address safely (FIXES ALL ERRORS)
     */
    private void setFromAddressSafely(Message message) throws MessagingException {
        try {
            // Try with personal name first
            message.setFrom(new InternetAddress(emailUsername, fromName, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            System.err.println("Error setting personal name, using email only: " + e.getMessage());
            // Fallback to email-only address
            message.setFrom(new InternetAddress(emailUsername));
        }
    }

    /**
     * Create OTP email message
     */
    private Message createOtpMessage(Session session, String toEmail, String otpCode) throws MessagingException {
        Message message = new MimeMessage(session);
        setFromAddressSafely(message);
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        message.setSubject("Password Reset - Verification Code");

        String emailContent = buildOtpEmailContent(otpCode);
        message.setContent(emailContent, "text/html; charset=utf-8");

        return message;
    }

    /**
     * Create password reset success message
     */
    private Message createPasswordResetSuccessMessage(Session session, String toEmail, String userName) throws MessagingException {
        Message message = new MimeMessage(session);
        setFromAddressSafely(message);
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        message.setSubject("Password Reset Successful");

        String emailContent = buildPasswordResetSuccessContent(userName);
        message.setContent(emailContent, "text/html; charset=utf-8");

        return message;
    }

    /**
     * Create welcome email message
     */
    private Message createWelcomeMessage(Session session, String toEmail, String userName) throws MessagingException {
        Message message = new MimeMessage(session);
        setFromAddressSafely(message);
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        message.setSubject("Welcome to Tailyatri!");

        String emailContent = buildWelcomeEmailContent(userName);
        message.setContent(emailContent, "text/html; charset=utf-8");

        return message;
    }

    /**
     * Create booking confirmation message (legacy)
     */
    private Message createBookingConfirmationMessage(Session session, String toEmail, String userName, String bookingDetails) throws MessagingException {
        Message message = new MimeMessage(session);
        setFromAddressSafely(message);
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        message.setSubject("Booking Confirmation - Tailyatri");

        String emailContent = buildBookingConfirmationContent(userName, bookingDetails);
        message.setContent(emailContent, "text/html; charset=utf-8");

        return message;
    }

    /**
     * Build booking confirmation email content with attachments info
     */
    private String buildBookingConfirmationWithAttachmentsContent(String userName, String pnr, String trainDetails, String journeyDetails) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Booking Confirmed - Tailyatri</title>
            </head>
            <body style="margin: 0; padding: 0; font-family: Arial, sans-serif; background-color: #f4f4f4;">
                <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="100%%">
                    <tr>
                        <td style="padding: 40px 0;">
                            <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="600" style="margin: 0 auto; background-color: white; border-radius: 15px; box-shadow: 0 10px 30px rgba(0,0,0,0.1);">
                                <!-- Header -->
                                <tr>
                                    <td style="background: linear-gradient(135deg, #10b981 0%%, #059669 100%%); padding: 40px 30px; text-align: center; border-radius: 15px 15px 0 0;">
                                        <h1 style="color: white; margin: 0; font-size: 32px; font-weight: 600;">🎫 Booking Confirmed!</h1>
                                        <p style="color: white; margin: 15px 0 0 0; font-size: 18px; opacity: 0.95;">Your journey awaits</p>
                                    </td>
                                </tr>
                                
                                <!-- Success Message -->
                                <tr>
                                    <td style="padding: 40px 30px 20px 30px; text-align: center;">
                                        <div style="background-color: #ecfdf5; border: 2px solid #10b981; border-radius: 12px; padding: 25px; margin-bottom: 30px;">
                                            <h2 style="color: #065f46; margin: 0 0 10px 0; font-size: 24px;">✅ Payment Successful!</h2>
                                            <p style="color: #047857; font-size: 16px; margin: 0; font-weight: 600;">Your train ticket has been booked successfully</p>
                                        </div>
                                    </td>
                                </tr>
                                
                                <!-- Content -->
                                <tr>
                                    <td style="padding: 0 30px 20px 30px;">
                                        <p style="color: #333; font-size: 18px; margin: 0 0 20px 0;">Hello <strong>%s</strong>! 👋</p>
                                        <p style="color: #666; font-size: 16px; line-height: 1.6; margin: 0 0 30px 0;">
                                            Great news! Your train booking has been confirmed and payment processed successfully. 
                                            Your e-ticket and invoice are attached to this email.
                                        </p>
                                        
                                        <!-- PNR Section -->
                                        <div style="background: linear-gradient(135deg, #3b82f6 0%%, #1e40af 100%%); border-radius: 12px; padding: 25px; text-align: center; margin: 30px 0;">
                                            <p style="color: white; margin: 0 0 8px 0; font-size: 14px; font-weight: 600; opacity: 0.9;">YOUR PNR NUMBER</p>
                                            <div style="color: white; font-size: 32px; font-weight: 700; letter-spacing: 3px; font-family: 'Courier New', monospace;">%s</div>
                                            <p style="color: white; margin: 15px 0 0 0; font-size: 14px; opacity: 0.9;">Keep this number for future reference</p>
                                        </div>
                                        
                                        <!-- Train Details -->
                                        <div style="background-color: #f8fafc; border: 1px solid #e2e8f0; border-radius: 12px; padding: 25px; margin: 30px 0;">
                                            <h3 style="color: #1f2937; margin: 0 0 20px 0; font-size: 20px; border-bottom: 2px solid #e5e7eb; padding-bottom: 10px;">🚂 Journey Details</h3>
                                            <div style="color: #374151; font-size: 15px; line-height: 1.8;">
                                                <div style="margin-bottom: 12px;"><strong>Train:</strong> %s</div>
                                                <div style="margin-bottom: 12px;"><strong>Journey:</strong> %s</div>
                                            </div>
                                        </div>
                                        
                                        <!-- Attachments Info -->
                                        <div style="background-color: #eff6ff; border: 2px dashed #3b82f6; border-radius: 12px; padding: 25px; margin: 30px 0;">
                                            <h3 style="color: #1e40af; margin: 0 0 15px 0; font-size: 18px;">📎 Important Documents Attached</h3>
                                            <div style="color: #1e40af; font-size: 14px;">
                                                <div style="margin-bottom: 10px;">📄 <strong>E-Ticket_%s.pdf</strong> - Your official train ticket</div>
                                                <div style="margin-bottom: 10px;">🧾 <strong>Invoice_%s.pdf</strong> - Payment receipt & invoice</div>
                                                <p style="margin: 15px 0 0 0; font-size: 13px; color: #3730a3;">
                                                    💡 <em>Download and keep these documents handy during your journey</em>
                                                </p>
                                            </div>
                                        </div>
                                        
                                        <!-- Important Instructions -->
                                        <div style="background-color: #fef3c7; border-left: 4px solid #f59e0b; padding: 20px; border-radius: 0 8px 8px 0; margin: 30px 0;">
                                            <h3 style="color: #92400e; margin: 0 0 15px 0; font-size: 16px;">⚠️ Travel Guidelines:</h3>
                                            <ul style="color: #92400e; margin: 0; padding-left: 20px; font-size: 14px; line-height: 1.6;">
                                                <li>Carry the printed e-ticket or show it on your mobile device</li>
                                                <li>Bring a valid government-issued photo ID</li>
                                                <li>Arrive at the station at least 30 minutes before departure</li>
                                                <li>Keep the invoice for any refund or modification requests</li>
                                            </ul>
                                        </div>
                                        
                                        <!-- CTA Section -->
                                        <div style="text-align: center; margin: 40px 0;">
                                            <p style="color: #666; font-size: 16px; margin: 0 0 20px 0;">Have a safe and comfortable journey!</p>
                                            <a href="https://tailyatri.com/my-bookings" 
                                               style="display: inline-block; background: linear-gradient(135deg, #3b82f6 0%%, #1e40af 100%%); 
                                                      color: white; text-decoration: none; padding: 15px 30px; border-radius: 8px; 
                                                      font-weight: 600; font-size: 16px; transition: all 0.3s;">
                                                View My Bookings
                                            </a>
                                        </div>
                                        
                                        <!-- Support -->
                                        <div style="text-align: center; margin: 30px 0;">
                                            <p style="color: #666; font-size: 14px; line-height: 1.6; margin: 0;">
                                                Need help? Contact our 24/7 support team at<br>
                                                📧 <a href="mailto:support@tailyatri.com" style="color: #3b82f6; text-decoration: none;">support@tailyatri.com</a> | 
                                                📞 <a href="tel:+911234567890" style="color: #3b82f6; text-decoration: none;">+91 123 456 7890</a>
                                            </p>
                                        </div>
                                    </td>
                                </tr>
                                
                                <!-- Footer -->
                                <tr>
                                    <td style="background-color: #f8fafc; padding: 30px; text-align: center; border-radius: 0 0 15px 15px; border-top: 1px solid #e2e8f0;">
                                        <p style="color: #6b7280; font-size: 12px; margin: 0 0 10px 0;">
                                            This email was sent by Tailyatri Train Booking System<br>
                                            📍 Your trusted journey partner since 2025
                                        </p>
                                        <p style="color: #9ca3af; font-size: 11px; margin: 0;">
                                            © 2025 Tailyatri. All rights reserved. | Terms of Service | Privacy Policy
                                        </p>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """.formatted(userName, pnr, trainDetails, journeyDetails, pnr, pnr);
    }

    // Keep all your existing content builder methods unchanged...
    private String buildOtpEmailContent(String otpCode) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Password Reset - Verification Code</title>
            </head>
            <body style="margin: 0; padding: 0; font-family: Arial, sans-serif; background-color: #f4f4f4;">
                <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="100%%">
                    <tr>
                        <td style="padding: 40px 0;">
                            <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="600" style="margin: 0 auto; background-color: white; border-radius: 10px; box-shadow: 0 4px 10px rgba(0,0,0,0.1);">
                                <!-- Header -->
                                <tr>
                                    <td style="background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); padding: 40px 30px; text-align: center; border-radius: 10px 10px 0 0;">
                                        <h1 style="color: white; margin: 0; font-size: 28px; font-weight: 300;">🚂 Tailyatri</h1>
                                        <p style="color: white; margin: 10px 0 0 0; font-size: 16px; opacity: 0.9;">Your Journey Partner</p>
                                    </td>
                                </tr>
                                
                                <!-- Content -->
                                <tr>
                                    <td style="padding: 40px 30px;">
                                        <h2 style="color: #333; margin: 0 0 20px 0; font-size: 24px; font-weight: 600;">Password Reset Request</h2>
                                        <p style="color: #666; font-size: 16px; line-height: 1.6; margin: 0 0 30px 0;">
                                            You requested to reset your password for your Tailyatri account. Use the verification code below to complete the process:
                                        </p>
                                        
                                        <!-- OTP Box -->
                                        <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="100%%">
                                            <tr>
                                                <td style="text-align: center; padding: 30px 0;">
                                                    <div style="background-color: #f8fafc; border: 2px dashed #3b82f6; border-radius: 12px; padding: 25px; display: inline-block;">
                                                        <p style="margin: 0 0 10px 0; color: #64748b; font-size: 14px; font-weight: 600;">VERIFICATION CODE</p>
                                                        <div style="font-size: 36px; font-weight: 700; color: #3b82f6; letter-spacing: 8px; font-family: 'Courier New', monospace;">%s</div>
                                                    </div>
                                                </td>
                                            </tr>
                                        </table>
                                        
                                        <!-- Important Notes -->
                                        <div style="background-color: #fef3c7; border-left: 4px solid #f59e0b; padding: 20px; border-radius: 0 8px 8px 0; margin: 30px 0;">
                                            <h3 style="color: #92400e; margin: 0 0 10px 0; font-size: 16px;">⚠️ Important Notes:</h3>
                                            <ul style="color: #92400e; margin: 0; padding-left: 20px; font-size: 14px;">
                                                <li>This code will expire in <strong>10 minutes</strong></li>
                                                <li>Do not share this code with anyone</li>
                                                <li>If you didn't request this, please ignore this email</li>
                                            </ul>
                                        </div>
                                        
                                        <!-- Support -->
                                        <p style="color: #666; font-size: 14px; line-height: 1.6; margin: 30px 0 0 0;">
                                            Need help? Contact our support team at 
                                            <a href="mailto:support@tailyatri.com" style="color: #3b82f6; text-decoration: none;">support@tailyatri.com</a>
                                        </p>
                                    </td>
                                </tr>
                                
                                <!-- Footer -->
                                <tr>
                                    <td style="background-color: #f8fafc; padding: 30px; text-align: center; border-radius: 0 0 10px 10px;">
                                        <p style="color: #94a3b8; font-size: 12px; margin: 0 0 10px 0;">
                                            This email was sent by Tailyatri Train Booking System
                                        </p>
                                        <p style="color: #94a3b8; font-size: 12px; margin: 0;">
                                            © 2025 Tailyatri. All rights reserved.
                                        </p>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """.formatted(otpCode);
    }

    private String buildPasswordResetSuccessContent(String userName) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <title>Password Reset Successful</title>
            </head>
            <body style="margin: 0; padding: 0; font-family: Arial, sans-serif; background-color: #f4f4f4;">
                <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="100%%">
                    <tr>
                        <td style="padding: 40px 0;">
                            <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="600" style="margin: 0 auto; background-color: white; border-radius: 10px;">
                                <tr>
                                    <td style="background: linear-gradient(135deg, #10b981 0%%, #059669 100%%); padding: 40px 30px; text-align: center; border-radius: 10px 10px 0 0;">
                                        <h1 style="color: white; margin: 0; font-size: 28px;">🚂 Tailyatri</h1>
                                        <p style="color: white; margin: 10px 0 0 0; font-size: 16px;">Password Reset Successful</p>
                                    </td>
                                </tr>
                                <tr>
                                    <td style="padding: 40px 30px;">
                                        <h2 style="color: #333; margin: 0 0 20px 0; text-align: center;">Password Reset Successful!</h2>
                                        <p style="color: #666; font-size: 16px; line-height: 1.6;">Hello %s,</p>
                                        <p style="color: #666; font-size: 16px; line-height: 1.6;">
                                            Your password has been successfully reset for your Tailyatri account. You can now log in using your new password.
                                        </p>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """.formatted(userName);
    }

    private String buildWelcomeEmailContent(String userName) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <title>Welcome to Tailyatri</title>
            </head>
            <body style="margin: 0; padding: 0; font-family: Arial, sans-serif; background-color: #f4f4f4;">
                <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="100%%">
                    <tr>
                        <td style="padding: 40px 0;">
                            <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="600" style="margin: 0 auto; background-color: white; border-radius: 10px;">
                                <tr>
                                    <td style="background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); padding: 40px 30px; text-align: center; border-radius: 10px 10px 0 0;">
                                        <h1 style="color: white; margin: 0; font-size: 28px;">🚂 Welcome to Tailyatri!</h1>
                                        <p style="color: white; margin: 10px 0 0 0; font-size: 16px;">Your Journey Begins Here</p>
                                    </td>
                                </tr>
                                <tr>
                                    <td style="padding: 40px 30px;">
                                        <h2 style="color: #333; margin: 0 0 20px 0;">Hello %s! 👋</h2>
                                        <p style="color: #666; font-size: 16px; line-height: 1.6;">
                                            Thank you for joining Tailyatri! We're excited to have you on board and help you with all your train booking needs.
                                        </p>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """.formatted(userName);
    }

    private String buildBookingConfirmationContent(String userName, String bookingDetails) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <title>Booking Confirmation</title>
            </head>
            <body style="margin: 0; padding: 0; font-family: Arial, sans-serif; background-color: #f4f4f4;">
                <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="100%%">
                    <tr>
                        <td style="padding: 40px 0;">
                            <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="600" style="margin: 0 auto; background-color: white; border-radius: 10px;">
                                <tr>
                                    <td style="background: linear-gradient(135deg, #10b981 0%%, #059669 100%%); padding: 40px 30px; text-align: center; border-radius: 10px 10px 0 0;">
                                        <h1 style="color: white; margin: 0; font-size: 28px;">🎫 Booking Confirmed!</h1>
                                        <p style="color: white; margin: 10px 0 0 0; font-size: 16px;">Your ticket is ready</p>
                                    </td>
                                </tr>
                                <tr>
                                    <td style="padding: 40px 30px;">
                                        <h2 style="color: #333; margin: 0 0 20px 0;">Hello %s!</h2>
                                        <p style="color: #666; font-size: 16px; line-height: 1.6;">
                                            Great news! Your train booking has been confirmed. Here are your ticket details:
                                        </p>
                                        <div style="background-color: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 25px; margin: 30px 0;">
                                            <h3 style="color: #333; margin: 0 0 15px 0; font-size: 18px;">Booking Details:</h3>
                                            <div style="color: #666; font-size: 14px; line-height: 1.6;">%s</div>
                                        </div>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """.formatted(userName, bookingDetails);
    }

    /**
     * Helper class for byte array data source
     */
    private static class ByteArrayDataSource implements DataSource {
        private byte[] data;
        private String contentType;

        public ByteArrayDataSource(byte[] data, String contentType) {
            this.data = data.clone(); // Create defensive copy
            this.contentType = contentType;
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(data);
        }

        @Override
        public OutputStream getOutputStream() {
            throw new UnsupportedOperationException("ByteArrayDataSource is read-only");
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public String getName() {
            return "ByteArrayDataSource";
        }
    }

    /**
     * Cleanup resources
     */
    public void shutdown() {
        emailExecutor.shutdown();
    }
}