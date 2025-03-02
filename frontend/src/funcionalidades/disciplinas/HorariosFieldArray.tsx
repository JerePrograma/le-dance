// src/componentes/disciplinas/HorariosFieldArray.tsx
import React from "react";
import { Field, FieldArray, ErrorMessage } from "formik";
import Boton from "../../componentes/comunes/Boton";
import type { DisciplinaHorarioRequest, DiaSemana } from "../../types/types";

interface HorariosFieldArrayProps {
    name: string;
    diasSemana: DiaSemana[];
}

const HorariosFieldArray: React.FC<HorariosFieldArrayProps> = ({ name, diasSemana }) => {
    return (
        <FieldArray name={name}>
            {({ push, remove, form }) => (
                <div>
                    {form.values.horarios && form.values.horarios.length > 0 ? (
                        form.values.horarios.map((_horario: DisciplinaHorarioRequest, index: number) => (
                            <div key={index} className="flex flex-col sm:flex-row gap-2 items-end mb-2 border p-2 rounded">
                                <div className="flex-1">
                                    <label className="block text-sm font-medium">Día</label>
                                    <Field as="select" name={`${name}.${index}.diaSemana`} className="form-input">
                                        <option value="">Seleccione un día</option>
                                        {diasSemana.map((dia) => (
                                            <option key={dia} value={dia}>
                                                {dia}
                                            </option>
                                        ))}
                                    </Field>
                                    <ErrorMessage name={`${name}.${index}.diaSemana`} component="div" className="text-red-500 text-xs" />
                                </div>
                                <div className="flex-1">
                                    <label className="block text-sm font-medium">Horario de Inicio</label>
                                    <Field name={`${name}.${index}.horarioInicio`} type="time" className="form-input" />
                                    <ErrorMessage name={`${name}.${index}.horarioInicio`} component="div" className="text-red-500 text-xs" />
                                </div>
                                <div className="flex-1">
                                    <label className="block text-sm font-medium">Duración (horas)</label>
                                    <Field name={`${name}.${index}.duracion`} type="number" step="0.5" className="form-input" />
                                    <ErrorMessage name={`${name}.${index}.duracion`} component="div" className="text-red-500 text-xs" />
                                </div>
                                <div>
                                    <Boton type="button" onClick={() => remove(index)} className="bg-red-500 text-white px-2 py-1 rounded">
                                        Eliminar
                                    </Boton>
                                </div>
                            </div>
                        ))
                    ) : (
                        <p className="text-sm text-gray-500">No se han agregado horarios.</p>
                    )}
                    <Boton type="button" onClick={() => push({ diaSemana: "", horarioInicio: "", duracion: 1 })} className="bg-blue-500 text-white px-2 py-1 rounded mt-2">
                        Agregar Horario
                    </Boton>
                </div>
            )}
        </FieldArray>
    );
};

export default HorariosFieldArray;
