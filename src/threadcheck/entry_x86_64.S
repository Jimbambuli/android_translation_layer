/* Common entry of every trampoline (index in %r11d): save the argument registers, let tc_enter() check the call
 * and return the real function, restore and jump there with the caller's stack untouched (stack arguments and
 * varargs, %al included, stay as they were). tc_enter() may replace the return address with tc_ret_stub to see
 * the return value (tc_exit()), which then returns to the original caller. */
	.text
	.globl tc_entry
	.hidden tc_entry
	.type tc_entry, @function
tc_entry:
	.cfi_startproc
	push %rbp
	.cfi_def_cfa_offset 16
	.cfi_offset %rbp, -16
	mov %rsp, %rbp
	.cfi_def_cfa_register %rbp
	sub $192, %rsp
	mov %rdi, 0(%rsp)
	mov %rsi, 8(%rsp)
	mov %rdx, 16(%rsp)
	mov %rcx, 24(%rsp)
	mov %r8, 32(%rsp)
	mov %r9, 40(%rsp)
	mov %rax, 48(%rsp)
	movdqu %xmm0, 64(%rsp)
	movdqu %xmm1, 80(%rsp)
	movdqu %xmm2, 96(%rsp)
	movdqu %xmm3, 112(%rsp)
	movdqu %xmm4, 128(%rsp)
	movdqu %xmm5, 144(%rsp)
	movdqu %xmm6, 160(%rsp)
	movdqu %xmm7, 176(%rsp)
	mov %r11, %rdi
	mov %rsp, %rsi
	lea 8(%rbp), %rdx
	call tc_enter
	mov %rax, %r11
	mov 0(%rsp), %rdi
	mov 8(%rsp), %rsi
	mov 16(%rsp), %rdx
	mov 24(%rsp), %rcx
	mov 32(%rsp), %r8
	mov 40(%rsp), %r9
	mov 48(%rsp), %rax
	movdqu 64(%rsp), %xmm0
	movdqu 80(%rsp), %xmm1
	movdqu 96(%rsp), %xmm2
	movdqu 112(%rsp), %xmm3
	movdqu 128(%rsp), %xmm4
	movdqu 144(%rsp), %xmm5
	movdqu 160(%rsp), %xmm6
	movdqu 176(%rsp), %xmm7
	leave
	.cfi_def_cfa %rsp, 8
	jmp *%r11
	.cfi_endproc
	.size tc_entry, .-tc_entry

	.globl tc_ret_stub
	.hidden tc_ret_stub
	.type tc_ret_stub, @function
tc_ret_stub:
	sub $48, %rsp
	mov %rax, 0(%rsp)
	mov %rdx, 8(%rsp)
	movdqu %xmm0, 16(%rsp)
	movdqu %xmm1, 32(%rsp)
	mov %rax, %rdi
	call tc_exit
	mov %rax, %r11
	mov 0(%rsp), %rax
	mov 8(%rsp), %rdx
	movdqu 16(%rsp), %xmm0
	movdqu 32(%rsp), %xmm1
	add $48, %rsp
	jmp *%r11
	.size tc_ret_stub, .-tc_ret_stub
	.section .note.GNU-stack,"",@progbits
