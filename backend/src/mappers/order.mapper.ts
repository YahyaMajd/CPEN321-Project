import { Order, CreateOrderResponse } from "../types/order.types";

/**
 * OrderMapper - Centralized data transformation for Order entities
 * Eliminates duplicate mapping logic across services
 */
export class OrderMapper {
  /**
   * Convert Order to CreateOrderResponse
   */
  static toCreateOrderResponse(order: any): CreateOrderResponse {
    return {
      _id: order._id,
      id: order._id.toString(),
      studentId: order.studentId.toString(),
      moverId: order.moverId?.toString(),
      status: order.status,
      volume: order.volume,
      price: order.price,
      studentAddress: order.studentAddress,
      warehouseAddress: order.warehouseAddress,
      returnAddress: order.returnAddress,
      pickupTime: order.pickupTime,
      returnTime: order.returnTime,
    };
  }

  /**
   * Convert Order to order list item
   */
  static toOrderListItem(order: any) {
    return {
      id: order._id.toString(),
      studentId: order.studentId.toString(),
      moverId: order.moverId?.toString(),
      status: order.status,
      volume: order.volume,
      totalPrice: order.price,
      studentAddress: order.studentAddress,
      warehouseAddress: order.warehouseAddress,
      returnAddress: order.returnAddress,
      pickupTime: order.pickupTime,
      returnTime: order.returnTime,
    };
  }

  /**
   * Convert array of Orders to order list items
   */
  static toOrderListItems(orders: any[]) {
    return orders.map(order => this.toOrderListItem(order));
  }
}
