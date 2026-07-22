package com.banquito.switchpagos.externalpayments.service;

import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class PayloadSanitizer {
    private static final Pattern ACCOUNT = Pattern.compile("(?i)((?:sourceAccountNumber|destinationAccountNumber)=)([^,)]*)");
    private static final Pattern BENEFICIARY_ID = Pattern.compile("(?i)(beneficiaryIdentification=)([^,)]*)");

    public String sanitize(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value);
        text = ACCOUNT.matcher(text).replaceAll(match -> match.group(1) + mask(match.group(2)));
        text = BENEFICIARY_ID.matcher(text).replaceAll(match -> match.group(1) + mask(match.group(2)));
        return text.length() > 4000 ? text.substring(0, 4000) : text;
    }

    private String mask(String value) {
        if (value == null || value.length() <= 4) {
            return "****";
        }
        return "****" + value.substring(value.length() - 4);
    }
}
