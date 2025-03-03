"use client"

import { useState } from "react"
import { Formik, Form, Field } from "formik"
import { toast } from "react-toastify"
import api from "../../api/axiosConfig"
import { Button } from "../../componentes/ui/button"
import { Input } from "../../componentes/ui/input"

interface Transaction {
    id: number
    detail: string
    type: "entrada" | "salida"
    number: string
    amount: number
}

export default function PettyCash() {
    const [transactions, setTransactions] = useState<Transaction[]>([])

    const total = transactions.reduce((acc, curr) => (curr.type === "entrada" ? acc + curr.amount : acc - curr.amount), 0)

    const initialValues = {
        startDate: "2025-02-01",
        endDate: "2025-02-24",
    }

    const handleFilter = async (values: { startDate: string; endDate: string }) => {
        try {
            const response = await api.get("/caja-chica", {
                params: { startDate: values.startDate, endDate: values.endDate },
            })
            setTransactions(response.data)
            toast.success("Transacciones cargadas correctamente.")
        } catch {
            toast.error("Error al cargar transacciones.")
        }
    }

    return (
        <div className="w-full max-w-4xl mx-auto bg-white rounded-lg shadow-md">
            <div className="p-4 border-b flex items-center justify-between">
                <h2 className="text-xl font-bold text-gray-900">Caja Chica</h2>
                <Button variant="outline" className="gap-2">
                    <CalendarIcon />
                    Exportar
                </Button>
            </div>

            <div className="p-4">
                <Formik initialValues={initialValues} onSubmit={handleFilter}>
                    {({ isSubmitting }) => (
                        <Form className="flex flex-col sm:flex-row gap-4 mb-4 items-center">
                            <div className="flex items-center gap-2">
                                <span className="text-sm text-gray-600">Fecha desde:</span>
                                <Field name="startDate">
                                    {({ field }: any) => (
                                        <Input
                                            type="date"
                                            className="w-40"
                                            value={field.value}
                                            onChange={field.onChange}
                                            name={field.name}
                                        />
                                    )}
                                </Field>
                            </div>
                            <div className="flex items-center gap-2">
                                <span className="text-sm text-gray-600">Fecha Hasta:</span>
                                <Field name="endDate">
                                    {({ field }: any) => (
                                        <Input
                                            type="date"
                                            className="w-40"
                                            value={field.value}
                                            onChange={field.onChange}
                                            name={field.name}
                                        />
                                    )}
                                </Field>
                            </div>
                            <Button type="submit" disabled={isSubmitting} variant="outline" className="gap-2">
                                <FilterIcon />
                                Filtrar
                            </Button>
                        </Form>
                    )}
                </Formik>

                <div className="rounded-md border border-gray-200">
                    <div className="overflow-x-auto">
                        <table className="w-full text-sm">
                            <thead>
                                <tr className="border-b bg-gray-50">
                                    <th className="py-2 px-4 text-left font-medium text-gray-600">Detalle</th>
                                    <th colSpan={2} className="text-center font-medium text-gray-600">
                                        ENTRADAS
                                    </th>
                                    <th colSpan={2} className="text-center font-medium text-gray-600">
                                        SALIDAS
                                    </th>
                                </tr>
                                <tr className="border-b bg-gray-50/50">
                                    <th className="py-2 px-4 text-left"></th>
                                    <th className="py-2 px-2 text-center font-medium text-gray-600">Nro</th>
                                    <th className="py-2 px-2 text-center font-medium text-gray-600">Importe</th>
                                    <th className="py-2 px-2 text-center font-medium text-gray-600">Nro</th>
                                    <th className="py-2 px-2 text-center font-medium text-gray-600">Importe</th>
                                </tr>
                            </thead>
                            <tbody className="bg-[#FFDAB9]/30">
                                {transactions.length === 0 ? (
                                    <tr>
                                        <td colSpan={5} className="py-4 text-center text-gray-500">
                                            No hay transacciones registradas
                                        </td>
                                    </tr>
                                ) : (
                                    transactions.map((transaction) => (
                                        <tr key={transaction.id} className="border-b hover:bg-gray-50">
                                            <td className="py-2 px-4">{transaction.detail}</td>
                                            {transaction.type === "entrada" ? (
                                                <>
                                                    <td className="py-2 px-2 text-center">{transaction.number}</td>
                                                    <td className="py-2 px-2 text-center">{transaction.amount.toFixed(2)}</td>
                                                    <td className="py-2 px-2"></td>
                                                    <td className="py-2 px-2"></td>
                                                </>
                                            ) : (
                                                <>
                                                    <td className="py-2 px-2"></td>
                                                    <td className="py-2 px-2"></td>
                                                    <td className="py-2 px-2 text-center">{transaction.number}</td>
                                                    <td className="py-2 px-2 text-center">{transaction.amount.toFixed(2)}</td>
                                                </>
                                            )}
                                        </tr>
                                    ))
                                )}
                            </tbody>
                        </table>
                    </div>
                </div>

                <div className="mt-4 flex flex-wrap gap-2 items-center justify-between">
                    <div className="flex gap-2">
                        <Button variant="outline" size="sm" className="gap-2">
                            <PrinterIcon />
                            Imprimir
                        </Button>
                        <Button variant="outline" size="sm" className="gap-2">
                            <PlusIcon />
                            Ingresos
                        </Button>
                        <Button variant="outline" size="sm" className="gap-2">
                            <MinusIcon />
                            Egresos
                        </Button>
                        <Button variant="outline" size="sm" className="gap-2">
                            <XIcon />
                            Cierre
                        </Button>
                    </div>
                    <div className="flex items-center gap-4">
                        <div className="text-sm font-semibold">TOTAL: {total.toFixed(2)}</div>
                        <Button variant="outline" size="sm" className="gap-2">
                            <HomeIcon />
                            Salir
                        </Button>
                    </div>
                </div>
            </div>
        </div>
    )
}

// Los componentes de iconos se mantienen igual que en tu codigo original
const CalendarIcon = () => (
    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"
        />
    </svg>
)

const FilterIcon = () => (
    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M3 4a1 1 0 011-1h16a1 1 0 011 1v2.586a1 1 0 01-.293.707l-6.414 6.414a1 1 0 00-.293.707V17l-4 4v-6.586a1 1 0 00-.293-.707L3.293 7.293A1 1 0 013 6.586V4z"
        />
    </svg>
)

const PrinterIcon = () => (
    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M17 17h2a2 2 0 002-2v-4a2 2 0 00-2-2H5a2 2 0 00-2 2v4a2 2 0 002 2h2m2 4h6a2 2 0 002-2v-4a2 2 0 00-2-2H9a2 2 0 00-2 2v4a2 2 0 002 2zm8-12V5a2 2 0 00-2-2H9a2 2 0 00-2 2v4h10z"
        />
    </svg>
)

const PlusIcon = () => (
    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
    </svg>
)

const MinusIcon = () => (
    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M20 12H4" />
    </svg>
)

const XIcon = () => (
    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
    </svg>
)

const HomeIcon = () => (
    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6"
        />
    </svg>
)

