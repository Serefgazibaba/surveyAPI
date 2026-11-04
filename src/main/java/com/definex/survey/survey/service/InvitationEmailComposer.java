package com.definex.survey.survey.service;

import com.definex.survey.mail.service.EmailContent;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

/** Davet maili içeriği (TR). Son tarih Europe/Istanbul saatiyle gösterilir (FR-50). */
@Component
public class InvitationEmailComposer {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
            .withZone(ZoneId.of("Europe/Istanbul"));

    public EmailContent compose(String recipientName, String surveyTitle, Instant endsAt, String invitationUrl) {
        String deadline = DATE.format(endsAt);
        String subject = surveyTitle + " anketine davetlisiniz";

        String text = """
                Merhaba %s,

                "%s" anketine davet edildiniz. Ankete aşağıdaki bağlantıdan katılabilirsiniz:

                %s

                Son katılım tarihi: %s

                Bu bağlantı size özeldir; lütfen başkalarıyla paylaşmayın.
                """.formatted(recipientName, surveyTitle, invitationUrl, deadline);

        String name = HtmlUtils.htmlEscape(recipientName);
        String title = HtmlUtils.htmlEscape(surveyTitle);
        String url = HtmlUtils.htmlEscape(invitationUrl);
        String html = """
                <!doctype html>
                <html lang="tr">
                <body style="margin:0;padding:24px;background:#f4f5f7;font-family:Arial,Helvetica,sans-serif;color:#1f2937">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0">
                    <tr><td align="center">
                      <table role="presentation" width="560" cellspacing="0" cellpadding="0"
                             style="background:#ffffff;border-radius:8px;padding:32px">
                        <tr><td>
                          <p style="margin:0 0 16px;font-size:16px">Merhaba %s,</p>
                          <p style="margin:0 0 24px;font-size:16px;line-height:1.5">
                            <strong>%s</strong> anketine davet edildiniz.
                          </p>
                          <p style="margin:0 0 24px">
                            <a href="%s" style="display:inline-block;background:#2563eb;color:#ffffff;
                               text-decoration:none;padding:12px 24px;border-radius:6px;font-weight:bold">Ankete katıl</a>
                          </p>
                          <p style="margin:0 0 8px;font-size:14px;color:#4b5563">Son katılım tarihi: <strong>%s</strong></p>
                          <p style="margin:0 0 24px;font-size:13px;color:#6b7280;word-break:break-all">
                            Buton çalışmazsa bu adresi tarayıcınıza yapıştırın:<br>%s
                          </p>
                          <p style="margin:0;font-size:12px;color:#9ca3af">
                            Bu bağlantı size özeldir; lütfen başkalarıyla paylaşmayın.
                          </p>
                        </td></tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(name, title, url, deadline, url);
        return new EmailContent(subject, html, text);
    }
}
