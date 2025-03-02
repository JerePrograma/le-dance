// useCobranzasData.ts
import { useEffect, useState } from "react";
import alumnosApi from "../api/alumnosApi";
import pagosApi from "../api/pagosApi";
import metodosPagoApi from "../api/metodosPagoApi";
import conceptosApi from "../api/conceptosApi";
import mensualidadesApi from "../api/mensualidadesApi";

export const useCobranzasData = () => {
  const [alumnos, setAlumnos] = useState([]);
  const [disciplinas, setDisciplinas] = useState([]);
  const [stocks, setStocks] = useState([]);
  const [metodosPago, setMetodosPago] = useState([]);
  const [conceptos, setConceptos] = useState([]);
  const [mensualidades, setMensualidades] = useState([]);

  useEffect(() => {
    alumnosApi.listar().then(setAlumnos).catch(console.error);
    pagosApi
      .listarDisciplinasBasicas()
      .then(setDisciplinas)
      .catch(console.error);
    pagosApi.listarStocksBasicos().then(setStocks).catch(console.error);
    metodosPagoApi
      .listarMetodosPago()
      .then((data) => setMetodosPago(Array.isArray(data) ? data : []))
      .catch(console.error);
    conceptosApi.listarConceptos().then(setConceptos).catch(console.error);
    mensualidadesApi
      .listarMensualidades()
      .then(setMensualidades)
      .catch(console.error);
  }, []);

  return {
    alumnos,
    disciplinas,
    stocks,
    metodosPago,
    conceptos,
    mensualidades,
  };
};
