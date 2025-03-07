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
import { toast } from "react-toastify";

export const useCobranzasData = () => {
  const [alumnos, setAlumnos] = useState<AlumnoListadoResponse[]>([]);
  const [disciplinas, setDisciplinas] = useState<DisciplinaListadoResponse[]>([]);
  const [stocks, setStocks] = useState<StockResponse[]>([]);
  const [metodosPago, setMetodosPago] = useState<MetodoPagoResponse[]>([]);
  const [conceptos, setConceptos] = useState<ConceptoResponse[]>([]);
  const [mensualidades, setMensualidades] = useState<MensualidadResponse[]>([]);

  useEffect(() => {
    alumnosApi.listar().then(setAlumnos).catch(toast.error);
    pagosApi.listarDisciplinasBasicas().then(setDisciplinas).catch(toast.error);
    pagosApi.listarStocksBasicos().then(setStocks).catch(toast.error);
    metodosPagoApi
      .listarMetodosPago()
      .then((data) => setMetodosPago(Array.isArray(data) ? data : []))
      .catch(toast.error);
    conceptosApi.listarConceptos().then(setConceptos).catch(toast.error);
    mensualidadesApi.listarMensualidades().then(setMensualidades).catch(toast.error);
  }, []);

  return { alumnos, disciplinas, stocks, metodosPago, conceptos, mensualidades };
};
