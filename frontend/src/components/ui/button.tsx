import { Slot } from '@radix-ui/react-slot'
import type { ButtonHTMLAttributes } from 'react'
import { cn } from '../../lib/utils'

type Props = ButtonHTMLAttributes<HTMLButtonElement> & { variant?: 'default' | 'outline' | 'ghost'; asChild?: boolean }

export function Button({ className, variant = 'default', asChild = false, ...props }: Props) {
  const Comp = asChild ? Slot : 'button'
  return <Comp className={cn('button', `button-${variant}`, className)} {...props} />
}
