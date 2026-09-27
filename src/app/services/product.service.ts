import { Injectable, inject } from '@angular/core';
import { Product } from '../models/product';
import { CategoryService } from './category.service';

@Injectable({
  providedIn: 'root',
})
export class ProductService {

  constructor() {}

  getProducts() {
    return this.productArray;
  }

  getActiveProducts() {
    return this.productArray.filter((product) => product.active);
  }

  getProductById(id: number) {
    return this.productArray.find((product) => product.id === id);
  }

  getPopularProducts() {
    return this.productArray.filter((product) => product.popular);
 }

  private categoryService = inject(CategoryService);
  private productArray: Product[] = [
    
    {
      id: 1,
      name: 'Tortilla Española',
      description:
        'Tortilla tradicional de patatas y cebolla, jugosa por dentro y dorada por fuera.',
      price: 18000,
      category: this.categoryService.getCategoryById(1)!,
      imageUrl:
        'https://images.cookforyourlife.org/wp-content/uploads/2018/05/Tortilla-Espanola-1-696x464.jpg',
      active: true,
      popular: false,
      vegetarian: true,
      spicyMild: false,
      spicyHot: false,
      containsNuts: false,
      containsSeafood: false,
      containsGluten: false,
    },

    {
      id: 2,
      name: 'Patatas Bravas',
      description:
        'Patatas crujientes acompañadas de salsa brava casera ligeramente picante.',
      price: 16000,
      category: this.categoryService.getCategoryById(1)!,
      imageUrl:
        'https://www.deliciousmagazine.co.uk/wp-content/uploads/2018/09/patatas-bravas.jpg',
      active: true,
      popular: false,
      vegetarian: true,
      spicyMild: true,
      spicyHot: false,
      containsNuts: false,
      containsSeafood: false,
      containsGluten: false,
    },

    {
      id: 3,
      name: 'Gambas al Ajillo',
      description:
        'Gambas salteadas en aceite de oliva con ajo, guindilla y perejil fresco.',
      price: 32000,
      category: this.categoryService.getCategoryById(2)!,
      imageUrl:
        'https://invitadoinvierno.com/wp-content/uploads/2019/07/receta-gambas-ajillo-0.jpg',
      active: true,
      popular: true,
      vegetarian: false,
      spicyMild: true,
      spicyHot: true,
      containsNuts: false,
      containsSeafood: true,
      containsGluten: false,
    },

    {
      id: 4,
      name: 'Calamares a la Romana',
      description:
        'Anillas de calamar rebozadas y fritas hasta quedar doradas y crujientes.',
      price: 28000,
      category: this.categoryService.getCategoryById(2)!,
      imageUrl:
        'https://newluxbrand.com/recetas/wp-content/uploads/2022/02/calamares-a-la-romana-Newlux.jpg',
      active: true,
      popular: false,
      vegetarian: false,
      spicyMild: false,
      spicyHot: false,
      containsNuts: false,
      containsSeafood: true,
      containsGluten: true,
    },

    {
      id: 5,
      name: 'Pulpo a la Brasa',
      description:
        'Tentáculo de pulpo a la brasa acompañado de patatas confitadas y aceite de oliva.',
      price: 42000,
      category: this.categoryService.getCategoryById(2)!,
      imageUrl:
        'https://esenciadelmar.es/wp-content/uploads/2023/08/pulpo-brasa-parilla-1200x700.jpeg',
      active: true,
      popular: true,
      vegetarian: false,
      spicyMild: false,
      spicyHot: false,
      containsNuts: false,
      containsSeafood: true,
      containsGluten: false,
    },

    {
      id: 6,
      name: 'Arroz Negro',
      description:
        'Arroz meloso cocinado con tinta de calamar, sepia, gambas y alioli casero.',
      price: 48000,
      category: this.categoryService.getCategoryById(2)!,
      imageUrl:
        'https://assets.tmecosys.com/image/upload/t_web_rdp_recipe_584x480/img/recipe/ras/Assets/f7f1b6c8-1a85-435e-b136-1e4cfa082903/Derivates/fbb52d55-2deb-4480-ac79-08b3522e4b09.jpg',
      active: true,
      popular: false,
      vegetarian: false,
      spicyMild: false,
      spicyHot: false,
      containsNuts: false,
      containsSeafood: true,
      containsGluten: false,
    },

    {
      id: 7,
      name: 'Fideuá Valenciana',
      description:
        'Fideos tostados cocinados con caldo de pescado, gambas, calamares y alioli.',
      price: 52000,
      category: this.categoryService.getCategoryById(2)!,
      imageUrl:
        'https://i0.wp.com/spainonafork.com/wp-content/uploads/2018/10/fideua3-11.png?fit=750%2C750&ssl=1',
      active: true,
      popular: true,
      vegetarian: false,
      spicyMild: false,
      spicyHot: false,
      containsNuts: false,
      containsSeafood: true,
      containsGluten: true,
    },

    {
      id: 8,
      name: 'Bacalao al Pil Pil',
      description:
        'Lomo de bacalao confitado en aceite de oliva con emulsión de ajo y guindilla.',
      price: 46000,
      category: this.categoryService.getCategoryById(2)!,
      imageUrl:
        'https://www.aceitesdeolivadeespana.com/wp-content/uploads/2020/09/bacalao-al-pil-pil.jpg',
      active: true,
      popular: false,
      vegetarian: false,
      spicyMild: true,
      spicyHot: false,
      containsNuts: false,
      containsSeafood: true,
      containsGluten: false,
    },

    {
      id: 9,
      name: 'Merluza a la Vasca',
      description:
        'Merluza fresca cocinada con salsa verde, almejas, espárragos y perejil.',
      price: 44000,
      category: this.categoryService.getCategoryById(2)!,
      imageUrl:
        'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRDV3yWMquqJyKwvVQG3hdrJ445tIeQrDw-3qUNt6Pox0VNHIWKldCADtz1&s=10',
      active: true,
      popular: false,
      vegetarian: false,
      spicyMild: false,
      spicyHot: false,
      containsNuts: false,
      containsSeafood: true,
      containsGluten: false,
    },

    {
      id: 10,
      name: 'Dorada al Horno',
      description:
        'Dorada fresca al horno con patatas, cebolla, limón y aceite de oliva virgen extra.',
      price: 39000,
      category: this.categoryService.getCategoryById(2)!,
      imageUrl:
        'https://lacocinadefrabisa.lavozdegalicia.es/wp-content/uploads/2016/03/dorada.jpg',
      active: true,
      popular: false,
      vegetarian: false,
      spicyMild: false,
      spicyHot: false,
      containsNuts: false,
      containsSeafood: true,
      containsGluten: false,
    },

    {
      id: 11,
      name: 'Cochinillo Segoviano',
      description:
        'Cochinillo asado lentamente hasta conseguir una piel crujiente y una carne tierna.',
      price: 68000,
      category: this.categoryService.getCategoryById(3)!,
      imageUrl:
        'https://www.tabladillo.es/wp-content/uploads/2024/07/Cochinillo-Tabladillo-Segovia-scaled.jpg',
      active: true,
      popular: true,
      vegetarian: false,
      spicyMild: false,
      spicyHot: false,
      containsNuts: false,
      containsSeafood: false,
      containsGluten: false,
    },

    {
      id: 12,
      name: 'Cordero Asado',
      description:
        'Pierna de cordero asada lentamente con hierbas aromáticas y patatas panaderas.',
      price: 62000,
      category: this.categoryService.getCategoryById(3)!,
      imageUrl:
        'https://assets.tmecosys.com/image/upload/t_web_rdp_recipe_584x480/img/recipe/ras/Assets/1C6B162D-43E7-4A9B-8690-3F873A07ED29/Derivates/D494B6BC-3836-410B-8455-B4555AAA565C.jpg',
      active: true,
      popular: false,
      vegetarian: false,
      spicyMild: false,
      spicyHot: false,
      containsNuts: false,
      containsSeafood: false,
      containsGluten: false,
    },

    {
      id: 13,
      name: 'Carrilleras de Cerdo',
      description:
        'Carrilleras de cerdo cocinadas a fuego lento en salsa de vino tinto y verduras.',
      price: 48000,
      category: this.categoryService.getCategoryById(3)!,
      imageUrl:
        'https://recetasdecocina.elmundo.es/wp-content/uploads/2025/12/carrilleras-de-cerdo-al-vino-tinto-1024x683.jpg',
      active: true,
      popular: false,
      vegetarian: false,
      spicyMild: false,
      spicyHot: false,
      containsNuts: false,
      containsSeafood: false,
      containsGluten: false,
    },

    {
      id: 14,
      name: 'Chorizo a la Sidra',
      description:
        'Chorizo asturiano cocinado lentamente en sidra natural hasta quedar jugoso y aromático.',
      price: 22000,
      category: this.categoryService.getCategoryById(1)!,
      imageUrl:
        'https://www.carolinescooking.com/wp-content/uploads/2023/06/chorizo-in-cider-featured-pic-sq.jpg',
      active: true,
      popular: false,
      vegetarian: false,
      spicyMild: false,
      spicyHot: false,
      containsNuts: false,
      containsSeafood: false,
      containsGluten: false,
    },

    {
      id: 15,
      name: 'Fabada Asturiana',
      description:
        'Guiso tradicional de fabes asturianas con chorizo, morcilla y panceta.',
      price: 38000,
      category: this.categoryService.getCategoryById(3)!,
      imageUrl: 'https://www.justspices.es/media/recipe/Fabada-asturiana.webp',
      active: true,
      popular: true,
      vegetarian: false,
      spicyMild: false,
      spicyHot: false,
      containsNuts: false,
      containsSeafood: false,
      containsGluten: false,
    },

    {
      id: 16,
      name: 'Callos a la Madrileña',
      description:
        'Guiso tradicional de callos de ternera con chorizo, morcilla y especias.',
      price: 36000,
      category: this.categoryService.getCategoryById(3)!,
      imageUrl:
        'https://static.bainet.es/clip/9932fc94-4a07-48d9-b06d-eb170c7f222c_source-aspect-ratio_1600w_0.jpg',
      active: true,
      popular: false,
      vegetarian: false,
      spicyMild: false,
      spicyHot: false,
      containsNuts: false,
      containsSeafood: false,
      containsGluten: false,
    },

    {
      id: 17,
      name: 'Rabo de Toro',
      description:
        'Estofado de rabo de toro cocinado lentamente con vino tinto, verduras y especias.',
      price: 58000,
      category: this.categoryService.getCategoryById(3)!,
      imageUrl:
        'https://www.foodandwine.com/thmb/1qa4CivgyDuYGgHVAM_0GYXkQgw=/1500x0/filters:no_upscale():max_bytes(150000):strip_icc()/rabo-de-toro-XL-RECIPE0917-5f241cba48de485b935f0e882b504fe5.jpg',
      active: true,
      popular: true,
      vegetarian: false,
      spicyMild: false,
      spicyHot: false,
      containsNuts: false,
      containsSeafood: false,
      containsGluten: false,
    },

    {
      id: 18,
      name: 'Pollo al Ajillo',
      description:
        'Pollo dorado en aceite de oliva con abundante ajo, vino blanco y perejil.',
      price: 32000,
      category: this.categoryService.getCategoryById(3)!,
      imageUrl:
        'https://cookingtheglobe.com/wp-content/uploads/2016/03/pollo-al-ajillo-garlic-chicken-recipe-2.jpg',
      active: true,
      popular: false,
      vegetarian: false,
      spicyMild: false,
      spicyHot: false,
      containsNuts: false,
      containsSeafood: false,
      containsGluten: false,
    },

    {
      id: 19,
      name: 'Secreto Ibérico',
      description:
        'Corte de cerdo ibérico a la parrilla servido con patatas y reducción de vino tinto.',
      price: 55000,
      category: this.categoryService.getCategoryById(3)!,
      imageUrl:
        'https://assets.tmecosys.com/image/upload/t_web_rdp_recipe_584x480/img/recipe/ras/Assets/58BEF3B8-6B5A-452E-9A4D-CEB1E38098A6/Derivates/c1a17709-3a16-4fa1-8c71-937c04747466.jpg',
      active: true,
      popular: true,
      vegetarian: false,
      spicyMild: false,
      spicyHot: false,
      containsNuts: false,
      containsSeafood: false,
      containsGluten: false,
    },

    {
      id: 20,
      name: 'Migas Extremeñas',
      description:
        'Migas de pan salteadas con ajo, pimentón, chorizo y panceta al estilo extremeño.',
      price: 28000,
      category: this.categoryService.getCategoryById(3)!,
      imageUrl:
        'https://recetasdecocina.elmundo.es/wp-content/uploads/2024/11/migas-extremenas-receta-1024x683.jpg',
      active: true,
      popular: false,
      vegetarian: false,
      spicyMild: false,
      spicyHot: false,
      containsNuts: false,
      containsSeafood: false,
      containsGluten: true,
    },

    {
      id: 21,
      name: 'Gazpacho Andaluz',
      description:
        'Sopa fría tradicional de tomate, pepino, pimiento, ajo y aceite de oliva.',
      price: 15000,
      category: this.categoryService.getCategoryById(1)!,
      imageUrl:
        'https://es-mycooktouch.group-taurus.com/image/recipe/540x391/gazpacho-andaluz',
      active: true,
      popular: false,
      vegetarian: true,
      spicyMild: false,
      spicyHot: false,
      containsNuts: false,
      containsSeafood: false,
      containsGluten: false,
    },

    {
      id: 22,
      name: 'Salmorejo Cordobés',
      description:
        'Crema fría de tomate y pan acompañada de huevo cocido y jamón serrano.',
      price: 17000,
      category: this.categoryService.getCategoryById(1)!,
      imageUrl:
        'https://www.aceitesdeolivadeespana.com/wp-content/uploads/2024/04/salmorejo_jamon_iberico.jpeg',
      active: true,
      popular: true,
      vegetarian: false,
      spicyMild: false,
      spicyHot: false,
      containsNuts: false,
      containsSeafood: false,
      containsGluten: true,
    },

    {
      id: 23,
      name: 'Pimientos del Piquillo Rellenos',
      description:
        'Pimientos del piquillo rellenos de bacalao y cubiertos con salsa de pimientos.',
      price: 26000,
      category: this.categoryService.getCategoryById(1)!,
      imageUrl:
        'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTq0RRc3EIfGd43qP4eJR-7bikx3GGdDG02z0DZkU4mt9U1gMqEQRFKZ9u7&s=10',
      active: true,
      popular: false,
      vegetarian: false,
      spicyMild: false,
      spicyHot: false,
      containsNuts: false,
      containsSeafood: true,
      containsGluten: false,
    },

    {
      id: 24,
      name: 'Berenjenas con Miel',
      description:
        'Berenjenas crujientes acompañadas de miel de caña y una pizca de sal marina.',
      price: 19000,
      category: this.categoryService.getCategoryById(1)!,
      imageUrl:
        'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQq2dnL2PshnrGDZfPPIZG-9l5EhRmdsfVZkBUUmKoE3w&s=10',
      active: true,
      popular: false,
      vegetarian: true,
      spicyMild: false,
      spicyHot: false,
      containsNuts: false,
      containsSeafood: false,
      containsGluten: false,
    },

    {
      id: 25,
      name: 'Ensaladilla Rusa',
      description:
        'Ensaladilla tradicional de patata, zanahoria, guisantes, huevo y atún con mayonesa casera.',
      price: 18000,
      category: this.categoryService.getCategoryById(1)!,
      imageUrl:
        'https://www.seriouseats.com/thmb/Qj2Ta8m0V7eHoqB0Z6Z1eFD3D-I=/1500x0/filters:no_upscale():max_bytes(150000):strip_icc()/20260515-SEA-RussianSalad-AmandaSuarez-15-7f6b215dd4434cf7aec5f3095cd09ed1.jpg',
      active: true,
      popular: false,
      vegetarian: false,
      spicyMild: false,
      spicyHot: false,
      containsNuts: false,
      containsSeafood: true,
      containsGluten: false,
    },

    {
      id: 26,
      name: 'Boquerones en Vinagre',
      description:
        'Boquerones marinados en vinagre con ajo, perejil y aceite de oliva virgen extra.',
      price: 21000,
      category: this.categoryService.getCategoryById(2)!,
      imageUrl:
        'https://img.turkishstylecooking.com/wp-content/uploads/2025/01/boquerones_en_vinagre1.jpg',
      active: true,
      popular: false,
      vegetarian: false,
      spicyMild: false,
      spicyHot: false,
      containsNuts: false,
      containsSeafood: true,
      containsGluten: false,
    },

    {
      id: 27,
      name: 'Pisto Manchego',
      description:
        'Guiso de tomate, calabacín, pimiento, cebolla y berenjena con aceite de oliva.',
      price: 22000,
      category: this.categoryService.getCategoryById(1)!,
      imageUrl:
        'https://www.aceitesdeolivadeespana.com/wp-content/uploads/2021/02/pisto-manchego-receta.jpg',
      active: true,
      popular: false,
      vegetarian: true,
      spicyMild: false,
      spicyHot: false,
      containsNuts: false,
      containsSeafood: false,
      containsGluten: false,
    },

    {
      id: 28,
      name: 'Huevos Rotos con Jamón',
      description:
        'Huevos fritos sobre patatas caseras acompañados de láminas de jamón ibérico.',
      price: 29000,
      category: this.categoryService.getCategoryById(3)!,
      imageUrl:
        'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSol8_W3dxVbXfF6nucJj4zoDIE2LXJwddqEjHa8WdfQw&s=10',
      active: true,
      popular: true,
      vegetarian: false,
      spicyMild: false,
      spicyHot: false,
      containsNuts: false,
      containsSeafood: false,
      containsGluten: false,
    },

    {
      id: 29,
      name: 'Bocadillo de Calamares',
      description:
        'Tradicional bocadillo madrileño de calamares fritos servido con alioli casero.',
      price: 23000,
      category: this.categoryService.getCategoryById(2)!,
      imageUrl:
        'https://www.tiaalia.com/wp-content/uploads/2017/11/bocadillo-de-calamares.jpg',
      active: true,
      popular: false,
      vegetarian: false,
      spicyMild: false,
      spicyHot: false,
      containsNuts: false,
      containsSeafood: true,
      containsGluten: true,
    },

    {
      id: 30,
      name: 'Cachopo Asturiano',
      description:
        'Filetes de ternera rellenos de jamón serrano y queso, empanados y fritos hasta quedar dorados.',
      price: 52000,
      category: this.categoryService.getCategoryById(3)!,
      imageUrl:
        'https://tablasdelcampillin.com/wp-content/uploads/2023/05/cachopos-capsa_4aH-Editar.jpg',
      active: true,
      popular: true,
      vegetarian: false,
      spicyMild: false,
      spicyHot: false,
      containsNuts: false,
      containsSeafood: false,
      containsGluten: true,
    },

    {
      id: 31,
      name: 'Crema Catalana',
      description:
        'Postre tradicional catalán de crema suave con una fina capa de azúcar caramelizado.',
      price: 16000,
      category: this.categoryService.getCategoryById(4)!,
      imageUrl:
        'https://assets.tmecosys.com/image/upload/t_web_rdp_recipe_584x480/img/recipe/ras/Assets/2d814c07a2145d881d1e701d4b27c029/Derivates/93949c61852021496362279d132f6996ec0ac30b.jpg',
      active: true,
      popular: false,
      vegetarian: true,
      spicyMild: false,
      spicyHot: false,
      containsNuts: false,
      containsSeafood: false,
      containsGluten: false,
    },

    {
      id: 32,
      name: 'Arroz con Leche',
      description:
        'Postre cremoso de arroz cocido lentamente con leche, canela y cáscara de limón.',
      price: 14000,
      category: this.categoryService.getCategoryById(4)!,
      imageUrl:
        'https://recetasdecocina.elmundo.es/wp-content/uploads/2024/11/arroz-con-leche-1024x683.jpg',
      active: true,
      popular: false,
      vegetarian: true,
      spicyMild: false,
      spicyHot: false,
      containsNuts: false,
      containsSeafood: false,
      containsGluten: false,
    },

    {
      id: 33,
      name: 'Churros con Chocolate',
      description:
        'Churros españoles crujientes acompañados de una taza de chocolate caliente y espeso.',
      price: 18000,
      category: this.categoryService.getCategoryById(4)!,
      imageUrl:
        'https://www.realsimple.com/thmb/74r1RaSWyOqGyLD9vakiGkmyIYQ=/1500x0/filters:no_upscale():max_bytes(150000):strip_icc()/churros-chocolate-0f3587a6a59f4696af610cafe100d1fe.jpg',
      active: true,
      popular: true,
      vegetarian: true,
      spicyMild: false,
      spicyHot: false,
      containsNuts: false,
      containsSeafood: false,
      containsGluten: true,
    },

    {
      id: 34,
      name: 'Torrijas',
      description:
        'Rebanadas de pan empapadas en leche aromatizada con canela y limón, doradas y espolvoreadas con azúcar.',
      price: 15000,
      category: this.categoryService.getCategoryById(4)!,
      imageUrl:
        'https://images.aws.nestle.recipes/resized/2024_10_28T12_32_34_badun_images.badun.es_dd1722b7f5d9_torrija_con_helado_de_torrija_y_coulis_de_frutos_rojos_1290_742.jpg',
      active: true,
      popular: false,
      vegetarian: true,
      spicyMild: false,
      spicyHot: false,
      containsNuts: false,
      containsSeafood: false,
      containsGluten: true,
    },

    {
      id: 35,
      name: 'Tarta de Queso Vasca',
      description:
        'Tarta de queso cremosa de interior suave y superficie caramelizada al estilo vasco.',
      price: 19000,
      category: this.categoryService.getCategoryById(4)!,
      imageUrl:
        'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSTPsxvBLFVs3SLnnc7A8F7_spaek91-Ghw1M3E8vb0ug&s=10',
      active: true,
      popular: true,
      vegetarian: true,
      spicyMild: false,
      spicyHot: false,
      containsNuts: false,
      containsSeafood: false,
      containsGluten: false,
    },

    {
      id: 36,
      name: 'Flan de Huevo',
      description:
        'Flan casero de huevo y leche servido con caramelo líquido y una textura delicadamente cremosa.',
      price: 14000,
      category: this.categoryService.getCategoryById(4)!,
      imageUrl:
        'https://static.bainet.es/clip/9c993f21-16d2-4148-bece-ba68ae1e172a_source-aspect-ratio_1600w_0.jpg',
      active: true,
      popular: false,
      vegetarian: true,
      spicyMild: false,
      spicyHot: false,
      containsNuts: false,
      containsSeafood: false,
      containsGluten: false,
    },

    {
      id: 37,
      name: 'Sangría Española',
      description:
        'Bebida tradicional española preparada con vino tinto, frutas frescas y un toque cítrico.',
      price: 18000,
      category: this.categoryService.getCategoryById(5)!,
      imageUrl:
        'https://descorcha.com/cdn/shop/articles/17380724881149_089e6f94-47d6-4852-b73d-a147c7542d85.jpg?v=1765379043',
      active: true,
      popular: true,
      vegetarian: false,
      spicyMild: false,
      spicyHot: false,
      containsNuts: false,
      containsSeafood: false,
      containsGluten: false,
    },

    {
      id: 38,
      name: 'Horchata de Chufa',
      description:
        'Bebida tradicional valenciana elaborada a base de chufa, fresca y ligeramente dulce.',
      price: 12000,
      category: this.categoryService.getCategoryById(5)!,
      imageUrl:
        'https://www.finedininglovers.es/sites/default/files/styles/1_1_768x768/public/recipe_content_images/horchata-de-chufa%C2%A9iStock.jpg.webp?itok=5pehvI1m',
      active: true,
      popular: false,
      vegetarian: false,
      spicyMild: false,
      spicyHot: false,
      containsNuts: false,
      containsSeafood: false,
      containsGluten: false,
    },

    {
      id: 39,
      name: 'Tinto de Verano',
      description:
        'Bebida refrescante preparada con vino tinto, gaseosa y un toque de limón.',
      price: 14000,
      category: this.categoryService.getCategoryById(5)!,
      imageUrl:
        'https://www.pequerecetas.com/wp-content/uploads/2022/06/como-hacer-tinto-de-verano.jpeg',
      active: true,
      popular: false,
      vegetarian: false,
      spicyMild: false,
      spicyHot: false,
      containsNuts: false,
      containsSeafood: false,
      containsGluten: false,
    },

    {
      id: 40,
      name: 'Café Cortado',
      description:
        'Café espresso servido con una pequeña cantidad de leche caliente.',
      price: 8000,
      category: this.categoryService.getCategoryById(5)!,
      imageUrl:
        'https://blogdelcafe.com/wp-content/uploads/2025/08/cortado-1024x647.webp',
      active: true,
      popular: true,
      vegetarian: false,
      spicyMild: false,
      spicyHot: false,
      containsNuts: false,
      containsSeafood: false,
      containsGluten: false,
    },
  ];
}
