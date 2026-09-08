# Terra Galega 🍽️

**Terra Galega** es un restaurante de **comida gallega** ubicado en Bogotá. En los últimos años ha ganado gran popularidad y, aunque cuenta con una única sede, la cantidad de pedidos ha crecido año tras año.

## El negocio

Actualmente el restaurante maneja domicilios, pero el proceso es completamente manual: el cliente llama por teléfono, un trabajador anota el pedido y todos los datos a mano, y posteriormente se completa el envío de forma manual y algo desorganizada. Terra Galega busca digitalizar este proceso a través de una página web que le permita:

- **Promocionar sus comidas**, mostrando el menú y la propuesta gastronómica del restaurante.
- **Permitir a los clientes realizar pedidos** de forma autónoma, sin necesidad de llamar.
- **Permitir a los trabajadores del restaurante monitorear el estado** de los pedidos a medida que avanzan.

La propuesta gastronómica de Terra Galega está centrada en la cocina tradicional de Galicia (España): entradas, mariscos, carnes, postres y bebidas típicas de esa región.

## Tipos de usuario

| Rol | Descripción |
|---|---|
| **Cliente** | Crea una cuenta suministrando nombre, apellido, correo, contraseña, teléfono y dirección. Explora el menú, agrega productos a su carrito de compras, selecciona adicionales y confirma sus pedidos. Puede consultar sus pedidos activos y su historial de compras. |
| **Operador** | Trabajador del restaurante encargado de actualizar el estado de los pedidos y asignarles un domiciliario. Ingresa al sistema con usuario y contraseña. |
| **Domiciliario** | Encargado de realizar la entrega de los pedidos. Cuenta con nombre, celular, cédula y un estado de disponibilidad. Es asignado automáticamente a los pedidos cuando pasan a estado "enviado". |
| **Administrador** | Ingresa a través de su propio portal y gestiona el negocio: da de alta, edita o retira operadores, domiciliarios, productos y adicionales del sistema. |

## El menú

Cada **producto** del restaurante tiene un nombre, un precio, una descripción y una imagen que lo representa. Los productos, además, pueden tener **adicionales** con los que el cliente personaliza su plato; un mismo adicional puede aplicar a varios productos, y estos pueden ser gratuitos o tener un costo adicional.

Cuando un cliente quiere varias unidades de un mismo producto, esto se refleja como una cantidad dentro de su pedido, en lugar de repetir el producto varias veces.

Las compras se pagan contra entrega o con datáfono directamente en la dirección de envío; no se realiza ningún pago a través de la página.

## El ciclo de vida de un pedido

1. El cliente selecciona uno o más productos, con sus respectivos adicionales y cantidades, y los agrega a su carrito.
2. Si el cliente cierra la página sin confirmar, al volver a entrar encuentra su carrito tal como lo dejó.
3. Al confirmar la compra, el pedido queda registrado en estado **recibido**, y se guarda su fecha de creación.
4. Un operador revisa los pedidos activos y cambia el estado a **cocinando** cuando el restaurante empieza a prepararlo.
5. Cuando el pedido está listo, el operador lo pasa a estado **enviado** y le asigna un domiciliario que esté disponible; ese domiciliario pasa entonces a estado "no disponible".
6. Cuando el domiciliario hace la entrega, el pedido pasa a estado **entregado**, se guarda su fecha de entrega, y el domiciliario vuelve a estar disponible para un nuevo envío.
7. El cliente puede revisar en cualquier momento sus pedidos activos y su historial de pedidos pasados. El restaurante, por su parte, puede consultar el historial general de todos los pedidos entregados, filtrando por rango de fechas o por producto.

## Gestión del negocio

Desde el portal de administración, el restaurante mantiene actualizado su catálogo y su equipo de trabajo:

- **Productos y adicionales:** se pueden crear, consultar, editar y "desactivar" (un producto desactivado no se elimina de la base de datos, simplemente deja de estar disponible para la compra).
- **Operadores:** se pueden registrar, consultar y eliminar.
- **Domiciliarios:** se pueden registrar, consultar, editar y desactivar del sistema.
