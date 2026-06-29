"use client";
import { useEffect } from "react";
import { Client, IMessage } from "@stomp/stompjs";

export default function useNotificacionesWebSocket(
  onMessage: (msg: unknown) => void
) {
  useEffect(() => {
    const client = new Client({
      brokerURL: "ws://localhost:8080/ws", // Asegúrate de que coincida con la configuración del backend
      reconnectDelay: 5000,
    });

    client.onConnect = () => {
      client.subscribe("/topic/notificaciones", (message: IMessage) => {
        const body = JSON.parse(message.body);
        onMessage(body);
      });
    };

    client.activate();

    return () => {
      client.deactivate();
    };
  }, [onMessage]);
}
