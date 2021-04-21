package org.jboss.pnc.scheduler.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.Singular;
import lombok.ToString;
import org.infinispan.protostream.annotations.ProtoDoc;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.jboss.pnc.scheduler.common.enums.Mode;
import org.jboss.pnc.scheduler.common.enums.State;
import org.jboss.pnc.scheduler.common.enums.StopFlag;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Task is an entity that holds data of remotely executed process.
 * <p>
 * TaskController manipulates Task's data.
 * <p>
 * Service has to be installed through BatchTaskInstaller that is provided by TaskTarget. After installation, Task's
 * data is held by Infinispan cache inside TaskRegistry/TaskContainer.
 *
 * @author Jan Michalov <jmichalo@redhat.com>
 */
@Setter
@ToString
@Builder(toBuilder = true)
@ProtoDoc("@Indexed")
@AllArgsConstructor(onConstructor_ = {@ProtoFactory})
public class Task {
    /**
     * Uniquely identifies a Task and serves as a key in Infinispan cache.
     */
    @Getter(onMethod_ = {@ProtoField(number = 1), @ProtoDoc("@Field(store=Store.YES)")})
    private final String name;

    /**
     * Holds data for communication with remote entity.
     * <p>
     * f.e. to start/stop remote execution
     */
    @Getter(onMethod_ = @ProtoField(number = 2))
    private RemoteAPI remoteEndpoints;

    /**
     * TaskController mode.
     */
    @Getter(onMethod_ = @ProtoField(number = 3))
    private Mode controllerMode;

    /**
     * Current state of a task. Default is State.IDLE.
     */
    @Getter(onMethod_ = {@ProtoField(number = 4), @ProtoDoc("@Field(store=Store.YES)")})
    private State state;

    /**
     * Tasks that are dependent on this task.
     * <p>
     * Parents of this Service.
     */
    @Singular
    @Getter(onMethod_ = @ProtoField(number = 5))
    private Set<String> dependants = new HashSet<>();

    /**
     * Number of unfinishedDependencies. Task can't remotely start if the number is positive.
     */
    @Getter(onMethod_ = {@ProtoField(number = 6, defaultValue = "-1")})
    private int unfinishedDependencies;

    @Singular
    @Getter(onMethod_ = @ProtoField(number = 7))
    private Set<String> dependencies = new HashSet<>();

    /**
     * Payload sent to remote entity.
     */
    @Getter(onMethod_ = @ProtoField(number = 8))
    private String payload;

    @Getter(onMethod_ = @ProtoField(number = 9))
    private StopFlag stopFlag;

    @Singular
    @Getter(onMethod_ = @ProtoField(number = 10))
    private List<ServerResponse> serverResponses = new ArrayList<>();

    @Getter(onMethod_ = @ProtoField(number = 11, defaultValue = "false"))
    private Boolean starting;

    public void incUnfinishedDependencies() {
        unfinishedDependencies++;
    }

    public void decUnfinishedDependencies() {
        unfinishedDependencies--;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Task task = (Task) o;
        return name.equals(task.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
