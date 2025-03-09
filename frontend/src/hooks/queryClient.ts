import { QueryClient } from "@tanstack/react-query";

// Configuración del cliente de React Query
export const queryClient = new QueryClient({
    defaultOptions: {
        queries: {
            refetchOnWindowFocus: false, // No recargar automáticamente al cambiar de ventana
            retry: 2, // Intentar dos veces en caso de error
            staleTime: 1000 * 60 * 5, // Considerar los datos frescos por 5 minutos
        },
    },
});
