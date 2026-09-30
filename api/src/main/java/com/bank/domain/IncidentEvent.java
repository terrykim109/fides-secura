package com.bank.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "incident_events")
@IdClass(IncidentEvent.Pk.class)
public class IncidentEvent {

    @Id
    @Column(name = "incident_id", nullable = false)
    private Long incidentId;

    @Id
    @Column(name = "security_event_id", nullable = false)
    private Long securityEventId;

    protected IncidentEvent() {
    }

    public IncidentEvent(Long incidentId, Long securityEventId) {
        this.incidentId = incidentId;
        this.securityEventId = securityEventId;
    }

    public Long getIncidentId() {
        return incidentId;
    }

    public Long getSecurityEventId() {
        return securityEventId;
    }

    public static final class Pk implements Serializable {
        private Long incidentId;
        private Long securityEventId;

        public Pk() {
        }

        public Pk(Long incidentId, Long securityEventId) {
            this.incidentId = incidentId;
            this.securityEventId = securityEventId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Pk pk)) {
                return false;
            }
            return Objects.equals(incidentId, pk.incidentId)
                    && Objects.equals(securityEventId, pk.securityEventId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(incidentId, securityEventId);
        }
    }
}
