package com.ikoyki.webtools.kanban.backend.entity;

public interface EnumInterface {
    // The interface safely assumes 'this' will be an Enum
    default String toPascalCase() {
        if (!(this instanceof Enum)) {
            throw new IllegalStateException("Only Enums can implement PascalCaseConvertible");
        }

        String[] words = ((Enum<?>) this).name().split("_");
        StringBuilder sb = new StringBuilder();

        for (String word : words) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1).toLowerCase());
            }
        }
        return sb.toString();
    }
}
