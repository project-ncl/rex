package org.jboss.pnc.scheduler.dto.requests;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Singular;
import org.jboss.pnc.scheduler.dto.CreateTaskDTO;
import org.jboss.pnc.scheduler.dto.EdgeDTO;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Map;
import java.util.Set;

@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class CreateGraphRequest {

    @Singular
    public Set<@NotNull @Valid EdgeDTO> edges;

    @Singular
    public Map<@NotBlank String, @NotNull @Valid CreateTaskDTO> vertices;
}
