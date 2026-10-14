import { Product } from '../models/product.model';

/**
 * Preview-only catalog data. The CatalogService uses this list when the API
 * cannot be reached, so the storefront remains demonstrable before services run.
 */
export const DEVELOPMENT_CATALOG: readonly Product[] = [
  {
    id: 'prod-loom-001',
    name: 'Handwoven Linen Throw',
    description: 'A lightweight, textured layer woven in small batches by a coastal studio.',
    category: 'Home',
    price: 86,
    currency: 'USD',
    rating: 4.9,
    reviewCount: 128,
    inventory: 14,
    seller: { id: 'seller-harbor', name: 'Harbor & Loom', rating: 4.9 },
    tags: ['linen', 'small batch', 'giftable'],
    featured: true
  },
  {
    id: 'prod-ceramic-002',
    name: 'Dune Breakfast Set',
    description: 'A calm, neutral stoneware set designed for slow weekend mornings.',
    category: 'Kitchen',
    price: 64,
    currency: 'USD',
    rating: 4.8,
    reviewCount: 94,
    inventory: 9,
    seller: { id: 'seller-clay', name: 'Second Clay', rating: 4.8 },
    tags: ['ceramic', 'handmade', 'tableware'],
    featured: true
  },
  {
    id: 'prod-carry-003',
    name: 'Field Notes Tote',
    description: 'A sturdy everyday carry with an interior pocket for your notebook and keys.',
    category: 'Bags',
    price: 48,
    currency: 'USD',
    rating: 4.7,
    reviewCount: 76,
    inventory: 22,
    seller: { id: 'seller-north', name: 'North Common', rating: 4.7 },
    tags: ['organic cotton', 'everyday', 'utility'],
    featured: false
  },
  {
    id: 'prod-care-004',
    name: 'Cedar & Citrus Candle',
    description: 'A bright, grounded scent poured by hand with a clean soy wax base.',
    category: 'Self-care',
    price: 28,
    currency: 'USD',
    rating: 4.6,
    reviewCount: 51,
    inventory: 31,
    seller: { id: 'seller-wick', name: 'Kindled Goods', rating: 4.6 },
    tags: ['soy wax', 'citrus', 'home fragrance'],
    featured: false
  },
  {
    id: 'prod-print-005',
    name: 'Tide Study Print',
    description: 'A limited-run risograph print inspired by tide pools and changing light.',
    category: 'Art',
    price: 36,
    currency: 'USD',
    rating: 4.9,
    reviewCount: 43,
    inventory: 7,
    seller: { id: 'seller-mar', name: 'Marrow Studio', rating: 4.9 },
    tags: ['print', 'limited run', 'blue'],
    featured: true
  },
  {
    id: 'prod-spice-006',
    name: 'Weeknight Spice Trio',
    description: 'Three bright blends to make everyday vegetables, grains, and soups sing.',
    category: 'Pantry',
    price: 24,
    currency: 'USD',
    rating: 4.5,
    reviewCount: 88,
    inventory: 18,
    seller: { id: 'seller-pantry', name: 'Common Table', rating: 4.5 },
    tags: ['spices', 'cooking', 'giftable'],
    featured: false
  }
];
