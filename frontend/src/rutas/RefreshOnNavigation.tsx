import { useEffect } from "react";
import { useLocation } from "react-router-dom";

const RefreshOnNavigation: React.FC = () => {
  const location = useLocation();

  useEffect(() => {
    const lastReloadPath = sessionStorage.getItem("lastReloadPath");
    // Si el path actual es distinto al que se recargó anteriormente, se recarga la página
    if (lastReloadPath !== location.pathname) {
      sessionStorage.setItem("lastReloadPath", location.pathname);
      window.location.reload();
    }
  }, [location.pathname]);

  return null;
};

export default RefreshOnNavigation;
