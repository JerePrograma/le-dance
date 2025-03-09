import React, { Suspense } from "react";
import { Routes, Route } from "react-router-dom";
import ProtectedRoute from "./ProtectedRoute";
import {
  publicRoutes,
  protectedRoutes,
  adminRoutes,
  otherProtectedRoutes,
} from "./routes";

const AppRouter: React.FC = () => {
  return (
    <Suspense fallback={<div>Cargando...</div>}>
      <Routes>
        {/* Rutas públicas */}
        {publicRoutes.map(({ path, Component }) => (
          <Route key={path} path={path} element={<Component />} />
        ))}

        {/* Rutas protegidas generales */}
        <Route element={<ProtectedRoute />}>
          {protectedRoutes.map(({ path, Component }) => (
            <Route key={path} path={path} element={<Component />} />
          ))}

          {/* Rutas solo para ADMINISTRADOR */}
          <Route element={<ProtectedRoute requiredRole="ADMINISTRADOR" />}>
            {adminRoutes.map(({ path, Component }) => (
              <Route key={path} path={path} element={<Component />} />
            ))}
          </Route>

          {/* Otras rutas protegidas */}
          {otherProtectedRoutes.map(({ path, Component }) => (
            <Route key={path} path={path} element={<Component />} />
          ))}
        </Route>
      </Routes>
    </Suspense>
  );
};

export default AppRouter;
