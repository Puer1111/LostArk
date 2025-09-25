package com.lostark.lostark.dto.character;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty; // Added import

import java.util.List;

@Data
public class CharacterAvatar {

    @JsonProperty("Type")
    private String type;
    @JsonProperty("Name")
    private String name;
    @JsonProperty("Icon")
    private String icon;
    @JsonProperty("Grade")
    private String grade;
    @JsonProperty("IsSet")
    private boolean isSet;
    @JsonProperty("IsInner")
    private boolean isInner;
    @JsonProperty("Tooltip")
    private String tooltip; // Storing as String due to complex nested JSON structure
}
