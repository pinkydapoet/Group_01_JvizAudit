package com.jvizaudit.entity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "code_history")
public class CodeHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer historyId;

    @JsonIgnore 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private String historyName;
    
    @Lob
    private String sourceCode;
    private String language = "Java";
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public Integer getHistoryId() { return historyId; }
    public String getHistoryName() { return historyName; }
    public String getSourceCode() { return sourceCode; }
    public String getLanguage() { return language; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUser(User user) { this.user = user; }
    public void setHistoryName(String historyName) { this.historyName = historyName; }
    public void setSourceCode(String sourceCode) { this.sourceCode = sourceCode; }
    public void setStatus(String status) { this.status = status; }
}