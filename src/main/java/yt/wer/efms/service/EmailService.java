package yt.wer.efms.service;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import yt.wer.efms.model.EmailTemplate;
import yt.wer.efms.repository.EmailTemplateRepository;

import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;

@Service
public class EmailService {
    private final JavaMailSender mailSender;
    private final EmailTemplateRepository emailTemplateRepository;
    private final String fromEmail;

    public EmailService(JavaMailSender mailSender, 
                        EmailTemplateRepository emailTemplateRepository,
                        @Value("${spring.mail.username:efms@werya.be}") String fromEmail) {
        this.mailSender = mailSender;
        this.emailTemplateRepository = emailTemplateRepository;
        this.fromEmail = fromEmail;
    }

    private final java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newCachedThreadPool();

    public void sendEmail(String to, String subject, String text) {
        executor.submit(() -> {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true);
                helper.setFrom(fromEmail);
                helper.setTo(to);
                helper.setSubject(subject);
                helper.setText(text, true); // true indicates html
                mailSender.send(message);
            } catch (MessagingException e) {
                e.printStackTrace();
            }
        });
    }

    public void sendTemplatedEmail(String to, String templateIdentifier, Map<String, String> variables, String fallbackSubject, String fallbackBody) {
        Optional<EmailTemplate> templateOpt = emailTemplateRepository.findByIdentifier(templateIdentifier);
        
        String subject = templateOpt.map(EmailTemplate::getSubject).orElse(fallbackSubject);
        String body = templateOpt.map(EmailTemplate::getBody).orElse(fallbackBody);

        for (Map.Entry<String, String> entry : variables.entrySet()) {
            subject = subject.replace("{{" + entry.getKey() + "}}", entry.getValue());
            body = body.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }

        sendEmail(to, subject, body);
    }
}
