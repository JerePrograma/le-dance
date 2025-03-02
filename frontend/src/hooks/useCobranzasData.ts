import { useEffect, useState } from "react";
import alumnosApi from "../api/alumnosApi";
import pagosApi from "../api/pagosApi";
import metodosPagoApi from "../api/metodosPagoApi";
import conceptosApi from "../api/conceptosApi";
import mensualidadesApi from "../api/mensualidadesApi";
import type {
  AlumnoListadoResponse,
  DisciplinaListadoResponse,
  StockResponse,
  MetodoPagoResponse,
  ConceptoResponse,
  MensualidadResponse,
} from "../types/types";

export const useCobranzasData = () => {
  const [alumnos, setAlumnos] = useState<AlumnoListadoResponse[]>([]);
  const [disciplinas, setDisciplinas] = useState<DisciplinaListadoResponse[]>([]);
  const [stocks, setStocks] = useState<StockResponse[]>([]);
  const [metodosPago, setMetodosPago] = useState<MetodoPagoResponse[]>([]);
  const [conceptos, setConceptos] = useState<ConceptoResponse[]>([]);
  const [mensualidades, setMensualidades] = useState<MensualidadResponse[]>([]);

  useEffect(() => {
    alumnosApi.listar().then(setAlumnos).catch(console.error);
    pagosApi.listarDisciplinasBasicas().then(setDisciplinas).catch(console.error);
    pagosApi.listarStocksBasicos().then(setStocks).catch(console.error);
    metodosPagoApi
      .listarMetodosPago()
      .then((data) => setMetodosPago(Array.isArray(data) ? data : []))
      .catch(console.error);
    conceptosApi.listarConceptos().then(setConceptos).catch(console.error);
    mensualidadesApi.listarMensualidades().then(setMensualidades).catch(console.error);
  }, []);

  return { alumnos, disciplinas, stocks, metodosPago, conceptos, mensualidades };
};
