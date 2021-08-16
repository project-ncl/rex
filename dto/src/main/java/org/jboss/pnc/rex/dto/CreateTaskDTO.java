package org.jboss.pnc.rex.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jboss.pnc.rex.common.enums.Mode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Builder(toBuilder = true)
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class CreateTaskDTO {

    @NotBlank
    public String name;

    @NotNull
    public HttpRequest remoteStart;

    @NotNull
    public HttpRequest remoteCancel;

    public HttpRequest callerNotifications;

    public Mode controllerMode;
}
