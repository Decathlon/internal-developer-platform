package com.decathlon.idp_core.infrastructure.adapters.persistence.model.audit;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import org.hibernate.envers.ModifiedEntityNames;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

@Entity
@Table(name = "envers_transaction_log")
@RevisionEntity(CustomRevisionListener.class)
public class CustomRevisionEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @RevisionNumber
  @Column(name = "rev")
  private long rev;

  @RevisionTimestamp
  @Column(name = "revision_timestamp")
  private long revisionTimestamp;

  @Column(name = "auth_id")
  private String authId;

  @ElementCollection
  @CollectionTable(name = "envers_modified_entities", joinColumns = @JoinColumn(name = "rev"))
  @Column(name = "entity_name")
  @ModifiedEntityNames
  private Set<String> modifiedEntityNames = new HashSet<>();

  public long getRev() {
    return rev;
  }

  public long getRevisionTimestamp() {
    return revisionTimestamp;
  }

  public String getAuthId() {
    return authId;
  }

  public void setAuthId(String authId) {
    this.authId = authId;
  }

  public Set<String> getModifiedEntityNames() {
    return modifiedEntityNames;
  }
}
