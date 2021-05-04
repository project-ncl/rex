package org.jboss.pnc.scheduler.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jboss.pnc.scheduler.common.enums.Method;

import java.util.List;

@NoArgsConstructor
@Builder
@Getter
@AllArgsConstructor
public class HttpRequest {

    public String url;

    public Method method;

    public List<HeaderDTO> headers;

    public Object attachment;
}
