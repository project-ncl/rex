package org.jboss.pnc.rex.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

@NoArgsConstructor
@Builder
@Getter
@AllArgsConstructor
public class EdgeDTO {

    @NotBlank(message = "The edge source is blank")
    public String source;

    @NotBlank(message = "The target source is blank")
    public String target;
}
