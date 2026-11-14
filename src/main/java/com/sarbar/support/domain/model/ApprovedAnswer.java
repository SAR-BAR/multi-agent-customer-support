package com.sarbar.support.domain.model;

public record ApprovedAnswer(
    Long id, GeneralType generalType, String topic, String answer, int policyVersion) {}
