CREATE TABLE carts
(
    id                 UUID                        NOT NULL,
    created_date       TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    last_modified_date TIMESTAMP WITHOUT TIME ZONE,
    customer_id        UUID,
    status             VARCHAR(255)                NOT NULL,
    CONSTRAINT pk_carts PRIMARY KEY (id)
);

CREATE TABLE categories
(
    id                 UUID                        NOT NULL,
    created_date       TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    last_modified_date TIMESTAMP WITHOUT TIME ZONE,
    name               VARCHAR(255)                NOT NULL,
    description        VARCHAR(255)                NOT NULL,
    parent_category_id UUID,
    CONSTRAINT pk_categories PRIMARY KEY (id)
);

CREATE TABLE customers
(
    id                 UUID                        NOT NULL,
    created_date       TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    last_modified_date TIMESTAMP WITHOUT TIME ZONE,
    first_name         VARCHAR(255)                NOT NULL,
    last_name          VARCHAR(255),
    email              VARCHAR(255)                NOT NULL,
    telephone          VARCHAR(255),
    enabled            BOOLEAN                     NOT NULL,
    CONSTRAINT pk_customers PRIMARY KEY (id)
);

CREATE TABLE order_items
(
    id                 UUID                        NOT NULL,
    created_date       TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    last_modified_date TIMESTAMP WITHOUT TIME ZONE,
    quantity           BIGINT                      NOT NULL,
    product_id         UUID,
    order_id           UUID,
    CONSTRAINT pk_order_items PRIMARY KEY (id)
);

CREATE TABLE orders
(
    id                 UUID                        NOT NULL,
    created_date       TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    last_modified_date TIMESTAMP WITHOUT TIME ZONE,
    total_price        DECIMAL(10, 2)              NOT NULL,
    status             VARCHAR(255)                NOT NULL,
    shipped            TIMESTAMP WITHOUT TIME ZONE,
    cart_id            UUID,
    address_1          VARCHAR(255),
    address_2          VARCHAR(255),
    city               VARCHAR(255),
    postal_code        VARCHAR(10)                 NOT NULL,
    country_code       VARCHAR(2)                  NOT NULL,
    CONSTRAINT pk_orders PRIMARY KEY (id)
);

CREATE TABLE payments
(
    id                   UUID                        NOT NULL,
    created_date         TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    last_modified_date   TIMESTAMP WITHOUT TIME ZONE,
    order_id             UUID                        NOT NULL,
    payment_reference_id VARCHAR(255)                NOT NULL,
    created_at           TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    status               VARCHAR(255)                NOT NULL,
    amount               DECIMAL                     NOT NULL,
    CONSTRAINT pk_payments PRIMARY KEY (id)
);

CREATE TABLE product_categories
(
    category_id UUID NOT NULL,
    product_id  UUID NOT NULL,
    CONSTRAINT pk_product_categories PRIMARY KEY (category_id, product_id)
);

CREATE TABLE products
(
    id                 UUID                        NOT NULL,
    created_date       TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    last_modified_date TIMESTAMP WITHOUT TIME ZONE,
    name               VARCHAR(255)                NOT NULL,
    description        VARCHAR(255)                NOT NULL,
    price              DECIMAL(10, 2)              NOT NULL,
    status             VARCHAR(255)                NOT NULL,
    sales_counter      INTEGER,
    CONSTRAINT pk_products PRIMARY KEY (id)
);

CREATE TABLE reviews
(
    id                 UUID                        NOT NULL,
    created_date       TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    last_modified_date TIMESTAMP WITHOUT TIME ZONE,
    title              VARCHAR(255)                NOT NULL,
    description        VARCHAR(255)                NOT NULL,
    rating             BIGINT                      NOT NULL,
    product_id         UUID                        NOT NULL,
    CONSTRAINT pk_reviews PRIMARY KEY (id)
);

ALTER TABLE product_categories
    ADD CONSTRAINT uc_3d0d0e623423b7bf1c17a4d5d UNIQUE (product_id, category_id);

ALTER TABLE categories
    ADD CONSTRAINT uc_categories_name UNIQUE (name);

ALTER TABLE payments
    ADD CONSTRAINT uc_payments_payment_reference UNIQUE (payment_reference_id);

ALTER TABLE carts
    ADD CONSTRAINT FK_CARTS_ON_CUSTOMER FOREIGN KEY (customer_id) REFERENCES customers (id);

ALTER TABLE categories
    ADD CONSTRAINT FK_CATEGORIES_ON_PARENT_CATEGORY FOREIGN KEY (parent_category_id) REFERENCES categories (id);

ALTER TABLE orders
    ADD CONSTRAINT FK_ORDERS_ON_CART FOREIGN KEY (cart_id) REFERENCES carts (id);

ALTER TABLE order_items
    ADD CONSTRAINT FK_ORDER_ITEMS_ON_ORDER FOREIGN KEY (order_id) REFERENCES orders (id);

ALTER TABLE order_items
    ADD CONSTRAINT FK_ORDER_ITEMS_ON_PRODUCT FOREIGN KEY (product_id) REFERENCES products (id);

ALTER TABLE payments
    ADD CONSTRAINT FK_PAYMENTS_ON_ORDER FOREIGN KEY (order_id) REFERENCES orders (id);

ALTER TABLE reviews
    ADD CONSTRAINT FK_REVIEWS_ON_PRODUCT FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE;

ALTER TABLE product_categories
    ADD CONSTRAINT fk_procat_on_category FOREIGN KEY (category_id) REFERENCES categories (id);

ALTER TABLE product_categories
    ADD CONSTRAINT fk_procat_on_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE;