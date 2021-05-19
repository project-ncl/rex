package org.jboss.pnc.scheduler.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;
import org.jboss.pnc.scheduler.common.enums.Method;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

@NoArgsConstructor
@Builder
@Getter
@AllArgsConstructor
public class HttpRequest {

    @URL
    public String url;

    @NotNull
    public Method method;

    public List<@Valid HeaderDTO> headers;

    public Object attachment;
}
