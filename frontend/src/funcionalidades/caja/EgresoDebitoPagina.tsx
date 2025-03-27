"use client"

import { useState, useEffect, useCallback, useMemo } from "react"
import { PlusCircle } from "lucide-react"
import { toast } from "react-toastify"
import Tabla from "../../componentes/comunes/Tabla"
import Pagination from "../../componentes/comunes/Pagination"
import { EgresoResponse, EgresoRegistroRequest } from "../../types/types"
import egresosApi from "../../api/egresosApi"
import Boton from "../../componentes/comunes/Boton"

export default function EgresosDebitoPagina() {
  // Estados para egresos, paginación y carga
  const [egresos, setEgresos] = useState<EgresoResponse[]>([])
  const [currentPage, setCurrentPage] = useState<number>(0)
  const [loading, setLoading] = useState<boolean>(false)
  const [error, setError] = useState<string | null>(null)
  const itemsPerPage = 5

  // Estados para el modal de agregar egreso
  const [showModal, setShowModal] = useState<boolean>(false)
  const [montoEgreso, setMontoEgreso] = useState<number>(0)
  const [obsEgreso, setObsEgreso] = useState<string>("")
  const [fecha, setFecha] = useState<string>(new Date().toISOString().split("T")[0])

  // Estados para filtros de fecha
  const [filterStartDate, setFilterStartDate] = useState<string>("")
  const [filterEndDate, setFilterEndDate] = useState<string>("")

  // Función para cargar egresos de tipo DEBITO
  const fetchEgresos = useCallback(async () => {
    try {
      setLoading(true)
      setError(null)
      const data = await egresosApi.listarEgresosDebito()
      setEgresos(data)
    } catch (err) {
      toast.error("Error al cargar egresos.")
      setError("Error al cargar egresos.")
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchEgresos()
  }, [fetchEgresos])

  // Filtrado por rango de fecha
  const filteredEgresos = useMemo(() => {
    return egresos.filter((egreso) => {
      const egresoDate = new Date(egreso.fecha)
      let valid = true
      if (filterStartDate) {
        valid = valid && egresoDate >= new Date(filterStartDate)
      }
      if (filterEndDate) {
        valid = valid && egresoDate <= new Date(filterEndDate)
      }
      return valid
    })
  }, [egresos, filterStartDate, filterEndDate])

  const pageCount = useMemo(() => Math.ceil(filteredEgresos.length / itemsPerPage), [filteredEgresos.length])
  const currentItems = useMemo(
    () =>
      filteredEgresos.slice(
        currentPage * itemsPerPage,
        (currentPage + 1) * itemsPerPage
      ),
    [filteredEgresos, currentPage]
  )

  const handlePageChange = useCallback(
    (newPage: number) => {
      if (newPage >= 0 && newPage < pageCount) {
        setCurrentPage(newPage)
      }
    },
    [pageCount]
  )

  const handleEliminar = async (id: number) => {
    try {
      await egresosApi.eliminarEgreso(id)
      fetchEgresos()
      toast.success("Egreso eliminado correctamente.")
    } catch (err) {
      toast.error("Error al eliminar egreso.")
    }
  }

  // Abrir y cerrar modal
  const handleAbrirModal = () => {
    setMontoEgreso(0)
    setObsEgreso("")
    setFecha(new Date().toISOString().split("T")[0])
    setShowModal(true)
  }

  const handleCerrarModal = () => {
    setShowModal(false)
  }

  // Guardar egreso (solo DEBITO)
  const handleGuardarEgreso = async () => {
    if (!montoEgreso || montoEgreso <= 0) {
      toast.error("El monto del egreso debe ser mayor a 0.")
      return
    }
    try {
      const nuevoEgreso: EgresoRegistroRequest = {
        fecha: fecha,
        monto: montoEgreso,
        observaciones: obsEgreso,
        metodoPagoId: 2, // ID correspondiente para DEBITO
      }
      await egresosApi.registrarEgreso(nuevoEgreso)
      toast.success("Egreso agregado correctamente.")
      setShowModal(false)
      fetchEgresos()
    } catch (err) {
      toast.error("Error al agregar egreso.")
    }
  }

  // Simulated navigation function
  const navigateToEdit = (id: number) => {
    console.log(`Navigating to edit page for egreso ${id}`)
    toast.info(`Navegando a editar egreso ${id}`)
  }

  if (loading) return <div className="p-4 text-center">Cargando...</div>
  if (error) return <div className="p-4 text-center text-red-600">{error}</div>

  // Renderizado personalizado para las filas de la tabla
  const renderRow = (item: EgresoResponse) => [
    item.id,
    new Date(item.fecha).toLocaleDateString("es-AR"),
    `$${item.monto.toLocaleString()}`,
    item.observaciones,
  ]

  // Acciones para cada fila
  const renderActions = (item: EgresoResponse) => (
    <>
      <Boton onClick={() => navigateToEdit(item.id)} className="bg-blue-500 text-white p-1 text-sm">
        Editar
      </Boton>
      <Boton onClick={() => handleEliminar(item.id)} className="bg-red-500 text-white p-1 text-sm">
        Eliminar
      </Boton>
    </>
  )

  return (
    <div className="page-container p-4">
      <h1 className="text-2xl font-bold mb-4">Egresos - Tipo DEBITO</h1>

      {/* Filtros por fecha */}
      <div className="flex gap-4 mb-4">
        <div>
          <label className="block font-medium">Fecha Inicio:</label>
          <input
            type="date"
            className="border p-2"
            value={filterStartDate}
            onChange={(e) => {
              setFilterStartDate(e.target.value)
              setCurrentPage(0)
            }}
          />
        </div>
        <div>
          <label className="block font-medium">Fecha Fin:</label>
          <input
            type="date"
            className="border p-2"
            value={filterEndDate}
            onChange={(e) => {
              setFilterEndDate(e.target.value)
              setCurrentPage(0)
            }}
          />
        </div>
      </div>

      <div className="flex justify-end mb-4">
        <Boton onClick={handleAbrirModal} className="bg-green-500 text-white p-2 flex items-center gap-2">
          <PlusCircle className="h-4 w-4" />
          Agregar Egreso
        </Boton>
      </div>

      <div className="border p-2">
        <Tabla
          headers={["ID", "Fecha", "Monto", "Observaciones"]}
          data={currentItems}
          customRender={renderRow}
          actions={renderActions}
          emptyMessage="No hay egresos registrados"
        />
      </div>

      {pageCount > 1 && (
        <div className="mt-4 flex justify-center">
          <Pagination
            currentPage={currentPage}
            totalPages={pageCount}
            onPageChange={handlePageChange}
            className="mt-4"
          />
        </div>
      )}

      {/* Modal para Agregar Egreso */}
      {showModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center">
          {/* Contenedor del formulario */}
          <div className="bg-black p-4 rounded shadow-md w-[300px]">
            <h2 className="text-xl font-bold mb-4">Nuevo Egreso DEBITO</h2>
            <div className="mb-2">
              <label className="block font-medium">Fecha:</label>
              <input
                type="date"
                className="border p-2 w-full"
                value={fecha}
                onChange={(e) => setFecha(e.target.value)}
              />
            </div>
            <div className="mb-2">
              <label className="block font-medium">Monto:</label>
              <input
                type="number"
                className="border p-2 w-full"
                value={montoEgreso || ""}
                onChange={(e) =>
                  setMontoEgreso(Number.parseFloat(e.target.value) || 0)
                }
              />
            </div>
            <div className="mb-2">
              <label className="block font-medium">Observaciones:</label>
              <input
                type="text"
                className="border p-2 w-full"
                value={obsEgreso}
                onChange={(e) => setObsEgreso(e.target.value)}
              />
            </div>
            <div className="flex justify-end gap-2 mt-4">
              <Boton onClick={handleCerrarModal}>
                Cancelar
              </Boton>
              <Boton onClick={handleGuardarEgreso}>
                Guardar
              </Boton>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
