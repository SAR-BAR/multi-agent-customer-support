-- ============================================================
-- V1: Create all tables for the multi-agent customer support system
-- ============================================================

-- ----------------------------
-- Domain tables
-- ----------------------------

CREATE TABLE customers (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    email      VARCHAR(255) NOT NULL UNIQUE,
    name       VARCHAR(255) NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE orders (
    id                   VARCHAR(36)  NOT NULL PRIMARY KEY,
    customer_id          BIGINT       NOT NULL,
    status               VARCHAR(20)  NOT NULL,
    total_paise          INT          NOT NULL,
    delivery_fee_paise   INT          NOT NULL DEFAULT 0,
    ordered_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expected_delivery_at TIMESTAMP    NULL,
    version              BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT fk_orders_customer FOREIGN KEY (customer_id) REFERENCES customers (id)
) ENGINE=InnoDB;

CREATE INDEX idx_orders_customer ON orders (customer_id);

CREATE TABLE order_items (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id         VARCHAR(36)  NOT NULL,
    product_name     VARCHAR(255) NOT NULL,
    quantity         INT          NOT NULL,
    unit_price_paise INT          NOT NULL,
    line_total_paise INT          NOT NULL,
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id)
) ENGINE=InnoDB;

CREATE INDEX idx_order_items_order ON order_items (order_id);

CREATE TABLE payments (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id        VARCHAR(36)  NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    amount_paise    INT          NOT NULL,
    method          VARCHAR(50)  NOT NULL,
    transaction_ref VARCHAR(100) NULL,
    paid_at         TIMESTAMP    NULL,
    CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders (id)
) ENGINE=InnoDB;

CREATE INDEX idx_payments_order ON payments (order_id);

CREATE TABLE shipments (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id        VARCHAR(36)  NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    carrier         VARCHAR(100) NULL,
    tracking_number VARCHAR(100) NULL,
    shipped_at      TIMESTAMP    NULL,
    delivered_at    TIMESTAMP    NULL,
    CONSTRAINT fk_shipments_order FOREIGN KEY (order_id) REFERENCES orders (id)
) ENGINE=InnoDB;

CREATE INDEX idx_shipments_order ON shipments (order_id);

CREATE TABLE invoices (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id    VARCHAR(36) NOT NULL,
    total_paise INT         NOT NULL,
    issued_at   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_invoices_order FOREIGN KEY (order_id) REFERENCES orders (id)
) ENGINE=InnoDB;

CREATE INDEX idx_invoices_order ON invoices (order_id);

CREATE TABLE invoice_lines (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_id       BIGINT       NOT NULL,
    description      VARCHAR(255) NOT NULL,
    quantity         INT          NOT NULL,
    unit_price_paise INT          NOT NULL,
    line_total_paise INT          NOT NULL,
    CONSTRAINT fk_invoice_lines_invoice FOREIGN KEY (invoice_id) REFERENCES invoices (id)
) ENGINE=InnoDB;

CREATE INDEX idx_invoice_lines_invoice ON invoice_lines (invoice_id);

CREATE TABLE memberships (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id   BIGINT      NOT NULL,
    status        VARCHAR(20) NOT NULL,
    free_delivery BOOLEAN     NOT NULL DEFAULT FALSE,
    started_at    TIMESTAMP   NOT NULL,
    expires_at    TIMESTAMP   NULL,
    CONSTRAINT fk_memberships_customer FOREIGN KEY (customer_id) REFERENCES customers (id)
) ENGINE=InnoDB;

CREATE INDEX idx_memberships_customer ON memberships (customer_id);

CREATE TABLE refunds (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id        VARCHAR(36)  NOT NULL,
    amount_paise    INT          NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL UNIQUE,
    refunded_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_refunds_order FOREIGN KEY (order_id) REFERENCES orders (id)
) ENGINE=InnoDB;

CREATE INDEX idx_refunds_order ON refunds (order_id);

CREATE TABLE credit_notes (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    credit_note_number VARCHAR(50) NOT NULL UNIQUE,
    order_id           VARCHAR(36) NOT NULL,
    amount_paise       INT         NOT NULL,
    issued_at          TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_credit_notes_order FOREIGN KEY (order_id) REFERENCES orders (id)
) ENGINE=InnoDB;

CREATE TABLE coupons (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    code           VARCHAR(50)  NOT NULL UNIQUE,
    order_id       VARCHAR(36)  NOT NULL,
    discount_paise INT          NOT NULL,
    issued_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at     TIMESTAMP    NULL,
    CONSTRAINT fk_coupons_order FOREIGN KEY (order_id) REFERENCES orders (id)
) ENGINE=InnoDB;

CREATE TABLE refund_policies (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    version        INT          NOT NULL UNIQUE,
    content        TEXT         NOT NULL,
    effective_from TIMESTAMP    NOT NULL
) ENGINE=InnoDB;

CREATE TABLE approved_answers (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    general_type   VARCHAR(30)  NOT NULL,
    topic          VARCHAR(255) NOT NULL,
    answer         TEXT         NOT NULL,
    policy_version INT          NOT NULL
) ENGINE=InnoDB;

CREATE INDEX idx_approved_answers_type ON approved_answers (general_type);

-- ----------------------------
-- Platform tables
-- ----------------------------

CREATE TABLE conversations (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT    NOT NULL,
    started_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_conversations_customer FOREIGN KEY (customer_id) REFERENCES customers (id)
) ENGINE=InnoDB;

CREATE INDEX idx_conversations_customer ON conversations (customer_id);

CREATE TABLE chat_memory (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT      NOT NULL,
    role            VARCHAR(20) NOT NULL,
    content         TEXT        NOT NULL,
    created_at      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_chat_memory_conversation FOREIGN KEY (conversation_id) REFERENCES conversations (id)
) ENGINE=InnoDB;

CREATE INDEX idx_chat_memory_conversation ON chat_memory (conversation_id);

CREATE TABLE cases (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT      NOT NULL,
    order_id        VARCHAR(36) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    opened_at       TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closed_at       TIMESTAMP   NULL,
    CONSTRAINT fk_cases_conversation FOREIGN KEY (conversation_id) REFERENCES conversations (id),
    CONSTRAINT fk_cases_order FOREIGN KEY (order_id) REFERENCES orders (id)
) ENGINE=InnoDB;

CREATE INDEX idx_cases_conversation ON cases (conversation_id);
CREATE INDEX idx_cases_order ON cases (order_id);

CREATE TABLE case_posts (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    case_id      BIGINT      NOT NULL,
    author       VARCHAR(20) NOT NULL,
    type         VARCHAR(30) NOT NULL,
    content_json JSON        NOT NULL,
    evidence_ids JSON        NULL,
    reply_to     BIGINT      NULL,
    created_at   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_case_posts_case FOREIGN KEY (case_id) REFERENCES cases (id),
    CONSTRAINT fk_case_posts_reply_to FOREIGN KEY (reply_to) REFERENCES case_posts (id)
) ENGINE=InnoDB;

CREATE INDEX idx_case_posts_case ON case_posts (case_id);

CREATE TABLE escalations (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    case_id     BIGINT    NOT NULL,
    reason      TEXT      NOT NULL,
    escalated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at  TIMESTAMP NULL,
    CONSTRAINT fk_escalations_case FOREIGN KEY (case_id) REFERENCES cases (id)
) ENGINE=InnoDB;

CREATE INDEX idx_escalations_case ON escalations (case_id);

CREATE TABLE saga_instances (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    case_id    BIGINT       NOT NULL,
    saga_type  VARCHAR(50)  NOT NULL,
    status     VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    started_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ended_at   TIMESTAMP    NULL,
    CONSTRAINT fk_saga_instances_case FOREIGN KEY (case_id) REFERENCES cases (id)
) ENGINE=InnoDB;

CREATE INDEX idx_saga_instances_case ON saga_instances (case_id);

CREATE TABLE saga_steps (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    saga_id         BIGINT       NOT NULL,
    step_name       VARCHAR(50)  NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    idempotency_key VARCHAR(100) NOT NULL UNIQUE,
    started_at      TIMESTAMP    NULL,
    completed_at    TIMESTAMP    NULL,
    CONSTRAINT fk_saga_steps_saga FOREIGN KEY (saga_id) REFERENCES saga_instances (id),
    CONSTRAINT uq_saga_step UNIQUE (saga_id, step_name)
) ENGINE=InnoDB;
