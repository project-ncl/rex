package org.jboss.pnc.rex.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@NoArgsConstructor
@Builder
@Getter
@AllArgsConstructor
public class HeaderDTO {

    @NotBlank(message = "Header name cannot be empty")
    public String name;

    @NotNull(message = "Header value cannot be missing")
    public String value;
}
