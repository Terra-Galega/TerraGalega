import { Client } from './client';

// Cómo se calcula el descuento (igual que DiscountType del back)
export type DiscountType = 'PERCENTAGE' | 'FIXED_AMOUNT';

// Cupón de descuento, con sus datos completos (igual que la entidad Coupon del back)
export interface Coupon {
    id: number;
    code: string;
    description: string | null;
    discountType: DiscountType;
    // Porcentaje (ej. 10) o monto fijo en pesos, según discountType
    discountValue: number;
    // Tope del descuento en cupones de porcentaje (null = sin tope)
    maxDiscount: number | null;
    // Subtotal mínimo del pedido para usarlo (null = sin mínimo)
    minOrderAmount: number | null;
    // Vigencia (null = sin límite por ese lado)
    startsAt: string | null;
    expiresAt: string | null;
    // Usos totales permitidos (null = ilimitado)
    maxUses: number | null;
    // true = se puede usar; false = desactivado
    active: boolean;
    // Dueño del cupón; null = cupón general. Del cliente solo vienen id y nombre
    client: Pick<Client, 'id' | 'name' | 'lastName'> | null;
}

// Lo que se envía al crear o editar: del dueño solo hace falta el id
export type CouponPayload = Omit<Coupon, 'id' | 'client'> & {
    client: { id: number } | null;
};

// Estado de un cupón para el cliente (lo calcula el back)
export type ClientCouponStatus = 'ACTIVE' | 'EXPIRED' | 'USED' | 'EXHAUSTED';

// Cupón tal como llega en "Mis cupones" (ClientCouponDTO del back)
export interface ClientCoupon {
    id: number;
    code: string;
    description: string | null;
    discountType: DiscountType;
    discountValue: number;
    maxDiscount: number | null;
    minOrderAmount: number | null;
    startsAt: string | null;
    expiresAt: string | null;
    // true = cupón general (sirve para cualquier cliente con el código)
    general: boolean;
    status: ClientCouponStatus;
}

// "Activos" = se pueden usar ahora; todo lo demás va en "Expirados"
export const isCouponActive = (coupon: ClientCoupon): boolean =>
    coupon.status === 'ACTIVE';

// Respuesta de validar un cupón en el checkout (CouponDiscountDTO del back):
// cuánto descontaría sobre el subtotal del carrito
export interface CouponDiscount {
    // Código ya normalizado por el back (en mayúsculas)
    code: string;
    description: string | null;
    // Monto en pesos que se descontaría del subtotal
    discount: number;
}