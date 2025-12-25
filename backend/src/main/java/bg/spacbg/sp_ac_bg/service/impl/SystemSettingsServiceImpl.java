package bg.spacbg.sp_ac_bg.service.impl;

import bg.spacbg.sp_ac_bg.model.dto.input.UpdateSmtpSettingsInput;
import bg.spacbg.sp_ac_bg.model.entity.SystemSettingsEntity;
import bg.spacbg.sp_ac_bg.repository.SystemSettingsRepository;
import bg.spacbg.sp_ac_bg.service.SystemSettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Properties;

@Service
@Transactional
public class SystemSettingsServiceImpl implements SystemSettingsService {

    private static final Logger log = LoggerFactory.getLogger(SystemSettingsServiceImpl.class);

    private final SystemSettingsRepository settingsRepository;

    public SystemSettingsServiceImpl(SystemSettingsRepository settingsRepository) {
        this.settingsRepository = settingsRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public SystemSettingsEntity getSettings() {
        return settingsRepository.getSettings();
    }

    @Override
    public SystemSettingsEntity updateSmtpSettings(UpdateSmtpSettingsInput input, String updatedBy) {
        SystemSettingsEntity settings = settingsRepository.getSettings();

        if (input.getSmtpHost() != null) {
            settings.setSmtpHost(input.getSmtpHost());
        }
        if (input.getSmtpPort() != null) {
            settings.setSmtpPort(input.getSmtpPort());
        }
        if (input.getSmtpUsername() != null) {
            settings.setSmtpUsername(input.getSmtpUsername());
        }
        if (input.getSmtpPassword() != null && !input.getSmtpPassword().isEmpty()) {
            // Само ако паролата не е празна - запазваме я
            // TODO: Encrypt password before saving
            settings.setSmtpPassword(input.getSmtpPassword());
        }
        if (input.getSmtpFromEmail() != null) {
            settings.setSmtpFromEmail(input.getSmtpFromEmail());
        }
        if (input.getSmtpFromName() != null) {
            settings.setSmtpFromName(input.getSmtpFromName());
        }
        if (input.getSmtpUseTls() != null) {
            settings.setSmtpUseTls(input.getSmtpUseTls());
        }
        if (input.getSmtpUseSsl() != null) {
            settings.setSmtpUseSsl(input.getSmtpUseSsl());
        }
        if (input.getSmtpEnabled() != null) {
            settings.setSmtpEnabled(input.getSmtpEnabled());
        }

        settings.setUpdatedBy(updatedBy);

        return settingsRepository.save(settings);
    }

    @Override
    public boolean testSmtpConnection(String testEmail) {
        SystemSettingsEntity settings = settingsRepository.getSettings();

        if (settings.getSmtpHost() == null || settings.getSmtpHost().isEmpty()) {
            throw new IllegalStateException("SMTP сървърът не е конфигуриран");
        }

        try {
            JavaMailSender mailSender = createMailSender(settings);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(settings.getSmtpFromEmail());
            message.setTo(testEmail);
            message.setSubject("SP-AC Тест на SMTP настройки");
            message.setText("Това е тестов имейл от SP-AC Accounting системата.\n\n" +
                    "Ако получавате това съобщение, SMTP настройките са конфигурирани правилно.");

            mailSender.send(message);
            log.info("Test email sent successfully to {}", testEmail);
            return true;

        } catch (Exception e) {
            log.error("Failed to send test email: {}", e.getMessage(), e);
            throw new RuntimeException("Грешка при изпращане на тестов имейл: " + e.getMessage());
        }
    }

    private JavaMailSender createMailSender(SystemSettingsEntity settings) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(settings.getSmtpHost());
        mailSender.setPort(settings.getSmtpPort() != null ? settings.getSmtpPort() : 587);
        mailSender.setUsername(settings.getSmtpUsername());
        mailSender.setPassword(settings.getSmtpPassword());

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");

        if (settings.isSmtpUseSsl()) {
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.socketFactory.port", String.valueOf(mailSender.getPort()));
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        } else if (settings.isSmtpUseTls()) {
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
        }

        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");
        props.put("mail.smtp.writetimeout", "5000");

        return mailSender;
    }
}
