CREATE TYPE "roles" AS ENUM (
  'admin',
  'user',
  'share'
);

CREATE TABLE "imported_parcels" (
  "id" integer PRIMARY KEY,
  "created_at" timestamp,
  "modified_at" timestamp,
  "date" timestamp,
  "geodata" polygon,
  "parcel" integer NOT NULL
);

CREATE TABLE "parcels" (
  "id" integer PRIMARY KEY,
  "created_at" timestamp,
  "modified_at" timestamp,
  "name" string,
  "active" bool,
  "start_validity" timestamp,
  "end_validity" timestamp,
  "geodata" polygon,
  "color" string,
  "corresponding_pac" integer NOT NULL,
  "parent_parcel" integer,
  "farm" integer,
  "period" integer
);

CREATE TABLE "periods" (
  "id" integer PRIMARY KEY,
  "created_at" timestamp,
  "modified_at" timestamp,
  "name" string,
  "start_date" timestamp,
  "end_date" timestamp,
  "farm" integer
);

CREATE TABLE "parcel_operations" (
  "id" integer PRIMARY KEY,
  "created_at" timestamp,
  "modified_at" timestamp,
  "date" timestamp,
  "duration_seconds" int,
  "type" integer,
  "parcels" integer
);

CREATE TABLE "operation_types" (
  "id" integer PRIMARY KEY,
  "created_at" timestamp,
  "modified_at" timestamp,
  "name" string,
  "default_tool" integer
);

CREATE TABLE "operation_products" (
  "id" integer PRIMARY KEY,
  "created_at" timestamp,
  "modified_at" timestamp,
  "quantity" double,
  "operation" integer,
  "product" integer,
  "unit" integer,
  "tool" integer
);

CREATE TABLE "attachments" (
  "id" integer PRIMARY KEY,
  "created_at" timestamp,
  "modified_at" timestamp,
  "filename" string,
  "content" blob,
  "parcel_id" integer
);

CREATE TABLE "product_types" (
  "id" integer PRIMARY KEY,
  "created_at" timestamp,
  "modified_at" timestamp,
  "name" string,
  "unit" integer
);

CREATE TABLE "products" (
  "id" integer PRIMARY KEY,
  "created_at" timestamp,
  "modified_at" timestamp,
  "name" string,
  "product_type_id" integer,
  "unit" integer,
  "farm" integer
);

CREATE TABLE "units" (
  "id" integer PRIMARY KEY,
  "created_at" timestamp,
  "modified_at" timestamp,
  "value" string
);

CREATE TABLE "tools" (
  "id" integer PRIMARY KEY,
  "created_at" timestamp,
  "modified_at" timestamp,
  "name" string,
  "category" integer,
  "farm" integer
);

CREATE TABLE "tool_categories" (
  "id" integer PRIMARY KEY,
  "created_at" timestamp,
  "modified_at" timestamp,
  "name" string
);

CREATE TABLE "farms" (
  "id" integer PRIMARY KEY,
  "created_at" timestamp,
  "modified_at" timestamp,
  "name" string,
  "description" string,
  "location" string,
  "is_public" bool,
  "show_name" bool,
  "show_description" bool,
  "show_location" bool
);

CREATE TABLE "farm_users" (
  "created_at" timestamp,
  "modified_at" timestamp,
  "farm" integer,
  "user" integer,
  "role" roles
);

CREATE TABLE "users" (
  "id" integer PRIMARY KEY,
  "created_at" timestamp,
  "modified_at" timestamp,
  "tutorial_state" string DEFAULT 'NOT_STARTED'
);

ALTER TABLE "imported_parcels" ADD FOREIGN KEY ("parcel") REFERENCES "parcels" ("id");

ALTER TABLE "parcels" ADD FOREIGN KEY ("corresponding_pac") REFERENCES "imported_parcels" ("id");

ALTER TABLE "parcels" ADD FOREIGN KEY ("parent_parcel") REFERENCES "parcels" ("id");

ALTER TABLE "parcels" ADD FOREIGN KEY ("farm") REFERENCES "farms" ("id");

ALTER TABLE "parcels" ADD FOREIGN KEY ("period") REFERENCES "periods" ("id");

ALTER TABLE "parcel_operations" ADD FOREIGN KEY ("type") REFERENCES "operation_types" ("id");

CREATE TABLE "parcels_parcel_operations" (
  "parcels_id" integer,
  "parcel_operations_parcels" integer,
  PRIMARY KEY ("parcels_id", "parcel_operations_parcels")
);

ALTER TABLE "parcels_parcel_operations" ADD FOREIGN KEY ("parcels_id") REFERENCES "parcels" ("id");

ALTER TABLE "parcels_parcel_operations" ADD FOREIGN KEY ("parcel_operations_parcels") REFERENCES "parcel_operations" ("parcels");


ALTER TABLE "operation_types" ADD FOREIGN KEY ("default_tool") REFERENCES "tools" ("id");

ALTER TABLE "operation_products" ADD FOREIGN KEY ("operation") REFERENCES "parcel_operations" ("id");

ALTER TABLE "operation_products" ADD FOREIGN KEY ("product") REFERENCES "products" ("id");

ALTER TABLE "operation_products" ADD FOREIGN KEY ("unit") REFERENCES "units" ("id");

ALTER TABLE "operation_products" ADD FOREIGN KEY ("tool") REFERENCES "tools" ("id");

ALTER TABLE "attachments" ADD FOREIGN KEY ("parcel_id") REFERENCES "parcels" ("id");

ALTER TABLE "product_types" ADD FOREIGN KEY ("unit") REFERENCES "units" ("id");

ALTER TABLE "products" ADD FOREIGN KEY ("product_type_id") REFERENCES "product_types" ("id");

ALTER TABLE "products" ADD FOREIGN KEY ("unit") REFERENCES "units" ("id");

ALTER TABLE "products" ADD FOREIGN KEY ("farm") REFERENCES "farms" ("id");

ALTER TABLE "tools" ADD FOREIGN KEY ("category") REFERENCES "tool_categories" ("id");

ALTER TABLE "tools" ADD FOREIGN KEY ("farm") REFERENCES "farms" ("id");

ALTER TABLE "farm_users" ADD FOREIGN KEY ("farm") REFERENCES "farms" ("id");

ALTER TABLE "farm_users" ADD FOREIGN KEY ("user") REFERENCES "users" ("id");
