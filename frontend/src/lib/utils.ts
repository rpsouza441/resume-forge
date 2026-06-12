import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';
import { format, formatDistanceToNow } from 'date-fns';

/**
 * Merge Tailwind classes with clsx + tailwind-merge.
 */
export function cn(...inputs: ClassValue[]): string {
  return twMerge(clsx(inputs));
}

/**
 * Format a date using date-fns.
 */
export function formatDate(date: Date | string, pattern = 'PP'): string {
  return format(new Date(date), pattern);
}

/**
 * Format a date as relative time (e.g., "2 hours ago").
 */
export function timeAgo(date: Date | string): string {
  return formatDistanceToNow(new Date(date), { addSuffix: true });
}

/**
 * Truncate a string to a maximum length.
 */
export function truncate(str: string, maxLength: number): string {
  if (str.length <= maxLength) return str;
  return str.slice(0, maxLength - 3) + '...';
}
