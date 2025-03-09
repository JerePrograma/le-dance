import { Field, FormikErrors, useFormikContext } from "formik";
import { AlumnoListadoResponse, CobranzasFormValues } from "../types/types";

const FormHeader: React.FC<{
    alumnos: AlumnoListadoResponse[];
    handleAlumnoChange: (
        alumnoIdStr: string,
        currentValues: CobranzasFormValues,
        setFieldValue: (field: string, value: any, shouldValidate?: boolean) => Promise<void | FormikErrors<CobranzasFormValues>>
    ) => void;
}> = ({ alumnos, handleAlumnoChange }) => {
    const { values, setFieldValue } = useFormikContext<CobranzasFormValues>();

    return (
        <div className="border p-4 mb-4">
            <h2 className="font-bold mb-2">Datos de Cobranza</h2>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                <div>
                    <label className="block font-medium">Recibo Nro:</label>
                    <Field name="reciboNro" readOnly className="border p-2 w-full" />
                </div>
                <div>
                    <label className="block font-medium">Alumno:</label>
                    <Field
                        as="select"
                        name="alumno"
                        className="border p-2 w-full"
                        onChange={(e: React.ChangeEvent<HTMLSelectElement>) => {
                            handleAlumnoChange(e.target.value, values, setFieldValue);
                        }}
                    >
                        <option value="">Seleccione un alumno</option>
                        {alumnos.map((alumno: AlumnoListadoResponse) => (
                            <option key={alumno.id} value={alumno.id}>
                                {alumno.nombre} {alumno.apellido}
                            </option>
                        ))}
                    </Field>
                </div>
                <div>
                    <label className="block font-medium">Fecha:</label>
                    <Field name="fecha" type="date" className="border p-2 w-full" />
                </div>
            </div>
        </div>
    );
};

export default FormHeader;
