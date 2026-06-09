// Domain types mirroring the OpenAPI component schemas in ../../openapi.yaml.
// Hand-written so the app is fully typed without a build-time codegen step.
// To regenerate `paths`/`components` types from the spec, run `npm run gen`
// (writes src/api/schema.d.ts via openapi-typescript).

export type UUID = string;

/** ISO-8601 offset date-time, e.g. "2022-03-10T12:15:50-04:00". */
export type ZonedDateTime = string;

export interface AddressDto {
  address1?: string;
  address2?: string;
  city?: string;
  postalCode: string;
  /** ISO 3166-1 alpha-2 country code. */
  countryCode?: string;
}

export interface CustomerDto {
  id?: UUID;
  firstName?: string;
  lastName?: string;
  email?: string;
  telephone?: string;
}

export interface CategoryDto {
  id?: UUID;
  name?: string;
  description?: string;
  parent_category_id?: UUID;
}

export interface ProductDto {
  id?: UUID;
  name?: string;
  description?: string;
  price?: number;
  status?: string;
  salesCounter?: number;
  /** Category ids this product belongs to. */
  categories?: UUID[];
}

export interface ReviewDto {
  id?: UUID;
  title?: string;
  description?: string;
  rating?: number;
}

export interface OrderItemDto {
  id?: UUID;
  unitPrice?: number;
  quantity?: number;
  productId?: UUID;
  orderId?: UUID;
}

export interface CartDto {
  id?: UUID;
  customerDto?: CustomerDto;
  status?: string;
}

export interface PaymentDto {
  id?: UUID;
  paymentReferenceId?: string;
  status?: string;
  orderId?: UUID;
}

export interface OrderDto {
  id?: UUID;
  price?: number;
  status?: string;
  shipped?: ZonedDateTime;
  paymentId?: UUID;
  shipmentAddress?: AddressDto;
  orderItems?: OrderItemDto[];
  cart?: CartDto;
}
