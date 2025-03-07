import React, { Component, ReactNode } from "react";
import { toast } from "react-toastify";

interface ErrorBoundaryProps {
    children: ReactNode;
}

interface ErrorBoundaryState {
    hasError: boolean;
}

class ErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
    constructor(props: ErrorBoundaryProps) {
        super(props);
        this.state = { hasError: false };
    }

    static getDerivedStateFromError(_: Error): ErrorBoundaryState {
        return { hasError: true };
    }

    componentDidCatch(_error: Error, _errorInfo: React.ErrorInfo) {
        toast.error("Error capturado en ErrorBoundary:");

        // 🔥 Redireccion manual al Dashboard despues de un pequeño retraso
        setTimeout(() => {
            window.location.href = "/";
        }, 100); // Redirige en 100ms (para evitar bloqueos de React)
    }

    render() {
        if (this.state.hasError) {
            return <h2>Ocurrio un error, redirigiendo al Dashboard...</h2>;
        }
        return this.props.children;
    }
}

export default ErrorBoundary;
