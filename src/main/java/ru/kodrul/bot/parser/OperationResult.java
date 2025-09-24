package ru.kodrul.bot.parser;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class OperationResult {

    private List<String> success = new ArrayList<>();
    private List<String> skipped = new ArrayList<>();
    private Map<String, String> failed = new HashMap<>();

    public void addSuccess(String user) {
        success.add(user);
    }

    public void addSkipped(String reason) {
        skipped.add(reason);
    }

    public void addFailed(String user, String error) {
        failed.put(user, error);
    }
}