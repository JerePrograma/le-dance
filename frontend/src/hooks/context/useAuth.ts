import { useContext } from "react";
import { AuthContext, type AuthContextProps } from "./auth-context";

export function useAuth(): AuthContextProps {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth must be used within an AuthProvider");
  return context;
}
