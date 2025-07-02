package com.example;

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;
import javafx.scene.control.TextField;

public class PreferencesHandler {
    private final List<String> prefKeywords = new ArrayList<>();
    private final List<TextField> keywordFields;

    public PreferencesHandler(List<TextField> keywordFields) {
        this.keywordFields = keywordFields;
        prefKeywords.addAll(loadKeywords());
    }

    public void saveKeywordsAction() {
        prefKeywords.clear();
        for (TextField field : keywordFields) {
            if (!field.getText().trim().isEmpty()) {
                prefKeywords.add(field.getText().trim());
            }
        }
        if (!prefKeywords.isEmpty()) {
            saveKeywords(prefKeywords);
        }
        UIHandler.alertBuilder("Preferences Updated", "Refresh To See Changes");
    }

    public void clearKeywordsAction() {
        for (TextField field : keywordFields) {
            field.clear();
        }
        prefKeywords.clear();
        saveKeywords(prefKeywords);
    }

    private void saveKeywords(List<String> keywords) {
        Preferences prefs = Preferences.userNodeForPackage(NewsAggregatorController.class);
        for (int i = 0; i < keywords.size(); i++) {
            prefs.put("keyword" + i, keywords.get(i));
        }
        for (int i = keywords.size(); i < 6; i++) {
            prefs.remove("keyword" + i);
        }
    }

    public List<String> loadKeywords() {
        Preferences prefs = Preferences.userNodeForPackage(NewsAggregatorController.class);
        List<String> keywords = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            String keyword = prefs.get("keyword" + i, null);
            if (keyword != null) {
                keywords.add(keyword);
            }
        }
        return keywords;
    }

    public List<String> getPrefKeywords() {
        return prefKeywords;
    }
}
