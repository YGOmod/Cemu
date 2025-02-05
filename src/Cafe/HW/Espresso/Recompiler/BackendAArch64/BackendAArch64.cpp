#include "BackendAArch64.h"

#pragma push_macro("CSIZE")
#undef CSIZE
#include <xbyak_aarch64.h>
#pragma pop_macro("CSIZE")
#include <xbyak_aarch64_util.h>

#include <cstddef>

#include "../PPCRecompiler.h"
#include "asm/x64util.h"
#include "Cafe/OS/libs/coreinit/coreinit_Time.h"
#include "Common/precompiled.h"
#include "Common/cpu_features.h"
#include "HW/Espresso/Interpreter/PPCInterpreterInternal.h"
#include "HW/Espresso/Interpreter/PPCInterpreterHelper.h"
#include "HW/Espresso/PPCState.h"

using namespace Xbyak_aarch64;

constexpr uint32_t TEMP_GPR_1_ID = 25;
constexpr uint32_t TEMP_GPR_2_ID = 26;
constexpr uint32_t PPC_RECOMPILER_INSTANCE_DATA_REG_ID = 27;
constexpr uint32_t MEMORY_BASE_REG_ID = 28;
constexpr uint32_t HCPU_REG_ID = 29;
constexpr uint32_t TEMP_FPR_1_ID = 29;
constexpr uint32_t TEMP_FPR_2_ID = 30;
constexpr uint32_t ASM_ROUTINE_FPR_ID = 31;

constexpr uint64_t DOUBLE_1_0 = std::bit_cast<uint64_t>(1.0);
static const XReg HCPU_REG{HCPU_REG_ID}, PPC_REC_INSTANCE_REG{PPC_RECOMPILER_INSTANCE_DATA_REG_ID}, MEM_BASE_REG{MEMORY_BASE_REG_ID};
static const XReg TEMP_GPR_1_XREG{TEMP_GPR_1_ID}, TEMP_GPR_2_XREG{TEMP_GPR_2_ID};
static const WReg TEMP_GPR_1_WREG{TEMP_GPR_1_ID}, TEMP_GPR_2_WREG{TEMP_GPR_2_ID};
static const VReg TEMP_FPR_1_VREG{TEMP_FPR_1_ID}, TEMP_FPR_2_VREG{TEMP_FPR_2_ID}, ASM_ROUTINE_FPR_VREG{ASM_ROUTINE_FPR_ID};
static const DReg TEMP_FPR_1_DREG{TEMP_FPR_1_ID}, TEMP_FPR_2_DREG{TEMP_FPR_2_ID};
static const SReg TEMP_FPR_1_SREG{TEMP_FPR_1_ID};
static const HReg TEMP_FPR_1_HREG{TEMP_FPR_1_ID};
static const QReg TEMP_FPR_1_QREG{TEMP_FPR_1_ID};
static const WReg LR_WREG{TEMP_GPR_2_ID};
static const XReg LR_XREG{TEMP_GPR_2_ID};

static const util::Cpu s_cpu;

struct AArch64GenContext_t : CodeGenerator, CodeContext
{
	AArch64GenContext_t();

	void enterRecompilerCode();
	void leaveRecompilerCode();

	void r_name(IMLInstruction* imlInstruction);
	void name_r(IMLInstruction* imlInstruction);
	bool r_s32(IMLInstruction* imlInstruction);
	bool r_r(IMLInstruction* imlInstruction);
	bool r_r_s32(IMLInstruction* imlInstruction);
	bool r_r_s32_carry(IMLInstruction* imlInstruction);
	bool r_r_r(IMLInstruction* imlInstruction);
	bool r_r_r_carry(IMLInstruction* imlInstruction);
	void compare(IMLInstruction* imlInstruction);
	void compare_s32(IMLInstruction* imlInstruction);
	bool load(IMLInstruction* imlInstruction, bool indexed);
	bool store(IMLInstruction* imlInstruction, bool indexed);
	void atomic_cmp_store(IMLInstruction* imlInstruction);
	bool macro(IMLInstruction* imlInstruction);
	bool fpr_load(IMLInstruction* imlInstruction, bool indexed);
	void psq_load(uint8 mode, VReg& dataReg, WReg& memReg, WReg& indexReg, sint32 memImmS32, bool indexed, const IMLReg& registerGQR = IMLREG_INVALID);
	void psq_load_generic(uint8 mode, VReg& dataReg, WReg& memReg, WReg& indexReg, sint32 memImmS32, bool indexed, const IMLReg& registerGQR);
	bool fpr_store(IMLInstruction* imlInstruction, bool indexed);
	void psq_store(uint8 mode, IMLRegID dataRegId, WReg& memReg, WReg& indexReg, sint32 memOffset, bool indexed, const IMLReg& registerGQR = IMLREG_INVALID);
	void psq_store_generic(uint8 mode, IMLRegID dataRegId, WReg& memReg, WReg& indexReg, sint32 memOffset, bool indexed, const IMLReg& registerGQR);
	void fpr_r_r(IMLInstruction* imlInstruction);
	void fpr_r_r_r(IMLInstruction* imlInstruction);
	void fpr_r_r_r_r(IMLInstruction* imlInstruction);
	void fpr_r(IMLInstruction* imlInstruction);
	void fpr_compare(IMLInstruction* imlInstruction);
	void cjump(const std::unordered_map<IMLSegment*, Label>& labels, IMLInstruction* imlInstruction, IMLSegment* imlSegment);
	void jump(const std::unordered_map<IMLSegment*, Label>& labels, IMLSegment* imlSegment);
	void conditionalJumpCycleCheck(const std::unordered_map<IMLSegment*, Label>& labels, IMLSegment* imlSegment);

	void gqr_generateScaleCode(VReg& dataReg, bool isLoad, bool scalePS1, const IMLReg& registerGQR);

	bool conditional_r_s32([[maybe_unused]] IMLInstruction* imlInstruction)
	{
		cemu_assert_unimplemented();
		return false;
	}
};

template<typename T>
T fpReg(uint32 index)
{
	return T(index - IMLArchAArch64::PHYSREG_FPR_BASE);
}

template<typename T>
T gpReg(uint32 index)
{
	return T(index - IMLArchAArch64::PHYSREG_GPR_BASE);
}

AArch64GenContext_t::AArch64GenContext_t()
	: CodeGenerator(DEFAULT_MAX_CODE_SIZE, AutoGrow)
{
}

void AArch64GenContext_t::r_name(IMLInstruction* imlInstruction)
{
	uint32 name = imlInstruction->op_r_name.name;
	auto regId = imlInstruction->op_r_name.regR.GetRegID();

	if (imlInstruction->op_r_name.regR.GetBaseFormat() == IMLRegFormat::I64)
	{
		WReg regR = gpReg<WReg>(regId);
		if (name >= PPCREC_NAME_R0 && name < PPCREC_NAME_R0 + 32)
		{
			ldr(regR, AdrImm(HCPU_REG, offsetof(PPCInterpreter_t, gpr) + sizeof(uint32) * (name - PPCREC_NAME_R0)));
		}
		else if (name >= PPCREC_NAME_SPR0 && name < PPCREC_NAME_SPR0 + 999)
		{
			uint32 sprIndex = (name - PPCREC_NAME_SPR0);
			if (sprIndex == SPR_LR)
				ldr(regR, AdrImm(HCPU_REG, offsetof(PPCInterpreter_t, spr.LR)));
			else if (sprIndex == SPR_CTR)
				ldr(regR, AdrImm(HCPU_REG, offsetof(PPCInterpreter_t, spr.CTR)));
			else if (sprIndex == SPR_XER)
				ldr(regR, AdrImm(HCPU_REG, offsetof(PPCInterpreter_t, spr.XER)));
			else if (sprIndex >= SPR_UGQR0 && sprIndex <= SPR_UGQR7)
				ldr(regR, AdrImm(HCPU_REG, offsetof(PPCInterpreter_t, spr.UGQR) + sizeof(PPCInterpreter_t::spr.UGQR[0]) * (sprIndex - SPR_UGQR0)));
			else
				cemu_assert_suspicious();
		}
		else if (name >= PPCREC_NAME_TEMPORARY && name < PPCREC_NAME_TEMPORARY + 4)
		{
			ldr(regR, AdrImm(HCPU_REG, offsetof(PPCInterpreter_t, temporaryGPR_reg) + sizeof(uint32) * (name - PPCREC_NAME_TEMPORARY)));
		}
		else if (name == PPCREC_NAME_XER_CA)
		{
			ldrb(regR, AdrImm(HCPU_REG, offsetof(PPCInterpreter_t, xer_ca)));
		}
		else if (name == PPCREC_NAME_XER_SO)
		{
			ldrb(regR, AdrImm(HCPU_REG, offsetof(PPCInterpreter_t, xer_so)));
		}
		else if (name >= PPCREC_NAME_CR && name <= PPCREC_NAME_CR_LAST)
		{
			ldrb(regR, AdrImm(HCPU_REG, offsetof(PPCInterpreter_t, cr) + (name - PPCREC_NAME_CR)));
		}
		else if (name == PPCREC_NAME_CPU_MEMRES_EA)
		{
			ldr(regR, AdrImm(HCPU_REG, offsetof(PPCInterpreter_t, reservedMemAddr)));
		}
		else if (name == PPCREC_NAME_CPU_MEMRES_VAL)
		{
			ldr(regR, AdrImm(HCPU_REG, offsetof(PPCInterpreter_t, reservedMemValue)));
		}
		else
		{
			cemu_assert_suspicious();
		}
	}
	else if (imlInstruction->op_r_name.regR.GetBaseFormat() == IMLRegFormat::F64)
	{
		QReg regR = fpReg<QReg>(imlInstruction->op_r_name.regR.GetRegID());
		if (name >= PPCREC_NAME_FPR0 && name < (PPCREC_NAME_FPR0 + 32))
		{
			mov(TEMP_GPR_1_XREG, offsetof(PPCInterpreter_t, fpr) + sizeof(FPR_t) * (name - PPCREC_NAME_FPR0));
			ldr(regR, AdrReg(HCPU_REG, TEMP_GPR_1_XREG));
		}
		else if (name >= PPCREC_NAME_TEMPORARY_FPR0 && name < (PPCREC_NAME_TEMPORARY_FPR0 + 8))
		{
			mov(TEMP_GPR_1_XREG, offsetof(PPCInterpreter_t, temporaryFPR) + sizeof(FPR_t) * (name - PPCREC_NAME_TEMPORARY_FPR0));
			ldr(regR, AdrReg(HCPU_REG, TEMP_GPR_1_XREG));
		}
		else
		{
			cemu_assert_debug(false);
		}
	}
	else
	{
		cemu_assert_suspicious();
	}
}

void AArch64GenContext_t::name_r(IMLInstruction* imlInstruction)
{
	uint32 name = imlInstruction->op_r_name.name;
	IMLRegID regId = imlInstruction->op_r_name.regR.GetRegID();

	if (imlInstruction->op_r_name.regR.GetBaseFormat() == IMLRegFormat::I64)
	{
		auto regR = gpReg<WReg>(regId);
		if (name >= PPCREC_NAME_R0 && name < PPCREC_NAME_R0 + 32)
		{
			str(regR, AdrImm(HCPU_REG, offsetof(PPCInterpreter_t, gpr) + sizeof(uint32) * (name - PPCREC_NAME_R0)));
		}
		else if (name >= PPCREC_NAME_SPR0 && name < PPCREC_NAME_SPR0 + 999)
		{
			uint32 sprIndex = (name - PPCREC_NAME_SPR0);
			if (sprIndex == SPR_LR)
				str(regR, AdrImm(HCPU_REG, offsetof(PPCInterpreter_t, spr.LR)));
			else if (sprIndex == SPR_CTR)
				str(regR, AdrImm(HCPU_REG, offsetof(PPCInterpreter_t, spr.CTR)));
			else if (sprIndex == SPR_XER)
				str(regR, AdrImm(HCPU_REG, offsetof(PPCInterpreter_t, spr.XER)));
			else if (sprIndex >= SPR_UGQR0 && sprIndex <= SPR_UGQR7)
				str(regR, AdrImm(HCPU_REG, offsetof(PPCInterpreter_t, spr.UGQR) + sizeof(PPCInterpreter_t::spr.UGQR[0]) * (sprIndex - SPR_UGQR0)));
			else
				cemu_assert_suspicious();
		}
		else if (name >= PPCREC_NAME_TEMPORARY && name < PPCREC_NAME_TEMPORARY + 4)
		{
			str(regR, AdrImm(HCPU_REG, offsetof(PPCInterpreter_t, temporaryGPR_reg) + sizeof(uint32) * (name - PPCREC_NAME_TEMPORARY)));
		}
		else if (name == PPCREC_NAME_XER_CA)
		{
			strb(regR, AdrImm(HCPU_REG, offsetof(PPCInterpreter_t, xer_ca)));
		}
		else if (name == PPCREC_NAME_XER_SO)
		{
			strb(regR, AdrImm(HCPU_REG, offsetof(PPCInterpreter_t, xer_so)));
		}
		else if (name >= PPCREC_NAME_CR && name <= PPCREC_NAME_CR_LAST)
		{
			strb(regR, AdrImm(HCPU_REG, offsetof(PPCInterpreter_t, cr) + (name - PPCREC_NAME_CR)));
		}
		else if (name == PPCREC_NAME_CPU_MEMRES_EA)
		{
			str(regR, AdrImm(HCPU_REG, offsetof(PPCInterpreter_t, reservedMemAddr)));
		}
		else if (name == PPCREC_NAME_CPU_MEMRES_VAL)
		{
			str(regR, AdrImm(HCPU_REG, offsetof(PPCInterpreter_t, reservedMemValue)));
		}
		else
		{
			cemu_assert_suspicious();
		}
	}
	else if (imlInstruction->op_r_name.regR.GetBaseFormat() == IMLRegFormat::F64)
	{
		QReg regR = fpReg<QReg>(imlInstruction->op_r_name.regR.GetRegID());
		if (name >= PPCREC_NAME_FPR0 && name < (PPCREC_NAME_FPR0 + 32))
		{
			mov(TEMP_GPR_1_XREG, offsetof(PPCInterpreter_t, fpr) + sizeof(FPR_t) * (name - PPCREC_NAME_FPR0));
			str(regR, AdrReg(HCPU_REG, TEMP_GPR_1_XREG));
		}
		else if (name >= PPCREC_NAME_TEMPORARY_FPR0 && name < (PPCREC_NAME_TEMPORARY_FPR0 + 8))
		{
			mov(TEMP_GPR_1_XREG, offsetof(PPCInterpreter_t, temporaryFPR) + sizeof(FPR_t) * (name - PPCREC_NAME_TEMPORARY_FPR0));
			str(regR, AdrReg(HCPU_REG, TEMP_GPR_1_XREG));
		}
		else
		{
			cemu_assert_debug(false);
		}
	}
	else
	{
		cemu_assert_suspicious();
	}
}

bool AArch64GenContext_t::r_r(IMLInstruction* imlInstruction)
{
	IMLRegID regRId = imlInstruction->op_r_r.regR.GetRegID();
	IMLRegID regAId = imlInstruction->op_r_r.regA.GetRegID();
	WReg regR = gpReg<WReg>(regRId);
	WReg regA = gpReg<WReg>(regAId);

	if (imlInstruction->operation == PPCREC_IML_OP_ASSIGN)
	{
		if (regRId != regAId)
			mov(regR, regA);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_ENDIAN_SWAP)
	{
		rev(regR, regA);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_ASSIGN_S8_TO_S32)
	{
		sxtb(regR, regA);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_ASSIGN_S16_TO_S32)
	{
		sxth(regR, regA);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_NOT)
	{
		mvn(regR, regA);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_NEG)
	{
		neg(regR, regA);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_CNTLZW)
	{
		clz(regR, regA);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_DCBZ)
	{
		movi(TEMP_FPR_1_VREG.d2, 0);
		if (regRId != regAId)
		{
			add(TEMP_GPR_1_WREG, regA, regR);
			and_(TEMP_GPR_1_WREG, TEMP_GPR_1_WREG, ~0x1f);
		}
		else
		{
			and_(TEMP_GPR_1_WREG, regA, ~0x1f);
		}
		add(TEMP_GPR_1_XREG, MEM_BASE_REG, TEMP_GPR_1_XREG);
		stp(TEMP_FPR_1_QREG, TEMP_FPR_1_QREG, AdrNoOfs(TEMP_GPR_1_XREG));
		return true;
	}
	else
	{
		cemuLog_log(LogType::Recompiler, "PPCRecompilerAArch64Gen_imlInstruction_r_r(): Unsupported operation {:x}", imlInstruction->operation);
		return false;
	}
	return true;
}

bool AArch64GenContext_t::r_s32(IMLInstruction* imlInstruction)
{
	sint32 imm32 = imlInstruction->op_r_immS32.immS32;
	WReg reg = gpReg<WReg>(imlInstruction->op_r_immS32.regR.GetRegID());

	if (imlInstruction->operation == PPCREC_IML_OP_ASSIGN)
	{
		mov(reg, imm32);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_LEFT_ROTATE)
	{
		ror(reg, reg, 32 - (imm32 & 0x1f));
	}
	else
	{
		cemuLog_log(LogType::Recompiler, "PPCRecompilerAArch64Gen_imlInstruction_r_s32(): Unsupported operation {:x}", imlInstruction->operation);
		return false;
	}
	return true;
}

bool AArch64GenContext_t::r_r_s32(IMLInstruction* imlInstruction)
{
	WReg regR = gpReg<WReg>(imlInstruction->op_r_r_s32.regR.GetRegID());
	WReg regA = gpReg<WReg>(imlInstruction->op_r_r_s32.regA.GetRegID());
	sint32 immS32 = imlInstruction->op_r_r_s32.immS32;

	if (imlInstruction->operation == PPCREC_IML_OP_ADD)
	{
		add_imm(regR, regA, immS32, TEMP_GPR_1_WREG);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_SUB)
	{
		sub_imm(regR, regA, immS32, TEMP_GPR_1_WREG);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_AND)
	{
		mov(TEMP_GPR_1_WREG, immS32);
		and_(regR, regA, TEMP_GPR_1_WREG);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_OR)
	{
		mov(TEMP_GPR_1_WREG, immS32);
		orr(regR, regA, TEMP_GPR_1_WREG);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_XOR)
	{
		mov(TEMP_GPR_1_WREG, immS32);
		eor(regR, regA, TEMP_GPR_1_WREG);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_RLWIMI)
	{
		uint32 vImm = (uint32)immS32;
		uint32 mb = (vImm >> 0) & 0xFF;
		uint32 me = (vImm >> 8) & 0xFF;
		uint32 sh = (vImm >> 16) & 0xFF;
		uint32 mask = ppc_mask(mb, me);
		if (sh)
		{
			ror(TEMP_GPR_1_WREG, regA, 32 - (sh & 0x1F));
			and_(TEMP_GPR_1_WREG, TEMP_GPR_1_WREG, mask);
		}
		else
		{
			and_(TEMP_GPR_1_WREG, regA, mask);
		}
		and_(regR, regR, ~mask);
		orr(regR, regR, TEMP_GPR_1_WREG);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_MULTIPLY_SIGNED)
	{
		mov(TEMP_GPR_1_WREG, immS32);
		mul(regR, regA, TEMP_GPR_1_WREG);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_LEFT_SHIFT)
	{
		lsl(regR, regA, (uint32)immS32 & 0x1f);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_RIGHT_SHIFT_U)
	{
		lsr(regR, regA, (uint32)immS32 & 0x1f);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_RIGHT_SHIFT_S)
	{
		asr(regR, regA, (uint32)immS32 & 0x1f);
	}
	else
	{
		cemuLog_log(LogType::Recompiler, "PPCRecompilerAArch64Gen_imlInstruction_r_r_s32(): Unsupported operation {:x}", imlInstruction->operation);
		cemu_assert_suspicious();
		return false;
	}
	return true;
}

bool AArch64GenContext_t::r_r_s32_carry(IMLInstruction* imlInstruction)
{
	WReg regR = gpReg<WReg>(imlInstruction->op_r_r_s32_carry.regR.GetRegID());
	WReg regA = gpReg<WReg>(imlInstruction->op_r_r_s32_carry.regA.GetRegID());
	WReg regCarry = gpReg<WReg>(imlInstruction->op_r_r_s32_carry.regCarry.GetRegID());

	sint32 immS32 = imlInstruction->op_r_r_s32_carry.immS32;
	if (imlInstruction->operation == PPCREC_IML_OP_ADD)
	{
		adds_imm(regR, regA, immS32, TEMP_GPR_1_WREG);
		cset(regCarry, Cond::CS);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_ADD_WITH_CARRY)
	{
		adds_imm(TEMP_GPR_1_WREG, regCarry, immS32, TEMP_GPR_1_WREG);
		adcs(regR, regA, TEMP_GPR_1_WREG);
		cset(regCarry, Cond::CS);
	}
	else
	{
		cemu_assert_suspicious();
		return false;
	}

	return true;
}

bool AArch64GenContext_t::r_r_r(IMLInstruction* imlInstruction)
{
	WReg regResult = gpReg<WReg>(imlInstruction->op_r_r_r.regR.GetRegID());
	XReg reg64Result = gpReg<XReg>(imlInstruction->op_r_r_r.regR.GetRegID());
	WReg regOperand1 = gpReg<WReg>(imlInstruction->op_r_r_r.regA.GetRegID());
	WReg regOperand2 = gpReg<WReg>(imlInstruction->op_r_r_r.regB.GetRegID());

	if (imlInstruction->operation == PPCREC_IML_OP_ADD)
	{
		add(regResult, regOperand1, regOperand2);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_SUB)
	{
		sub(regResult, regOperand1, regOperand2);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_OR)
	{
		orr(regResult, regOperand1, regOperand2);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_AND)
	{
		and_(regResult, regOperand1, regOperand2);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_XOR)
	{
		eor(regResult, regOperand1, regOperand2);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_MULTIPLY_SIGNED)
	{
		mul(regResult, regOperand1, regOperand2);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_SLW)
	{
		tst(regOperand2, 32);
		lsl(regResult, regOperand1, regOperand2);
		csel(regResult, regResult, wzr, Cond::EQ);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_SRW)
	{
		tst(regOperand2, 32);
		lsr(regResult, regOperand1, regOperand2);
		csel(regResult, regResult, wzr, Cond::EQ);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_LEFT_ROTATE)
	{
		neg(TEMP_GPR_1_WREG, regOperand2);
		ror(regResult, regOperand1, TEMP_GPR_1_WREG);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_RIGHT_SHIFT_S)
	{
		asr(regResult, regOperand1, regOperand2);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_RIGHT_SHIFT_U)
	{
		lsr(regResult, regOperand1, regOperand2);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_LEFT_SHIFT)
	{
		lsl(regResult, regOperand1, regOperand2);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_DIVIDE_SIGNED)
	{
		sdiv(regResult, regOperand1, regOperand2);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_DIVIDE_UNSIGNED)
	{
		udiv(regResult, regOperand1, regOperand2);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_MULTIPLY_HIGH_SIGNED)
	{
		smull(reg64Result, regOperand1, regOperand2);
		lsr(reg64Result, reg64Result, 32);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_MULTIPLY_HIGH_UNSIGNED)
	{
		umull(reg64Result, regOperand1, regOperand2);
		lsr(reg64Result, reg64Result, 32);
	}
	else
	{
		cemuLog_log(LogType::Recompiler, "PPCRecompilerAArch64Gen_imlInstruction_r_r_r(): Unsupported operation {:x}", imlInstruction->operation);
		return false;
	}
	return true;
}

bool AArch64GenContext_t::r_r_r_carry(IMLInstruction* imlInstruction)
{
	WReg regR = gpReg<WReg>(imlInstruction->op_r_r_r_carry.regR.GetRegID());
	WReg regA = gpReg<WReg>(imlInstruction->op_r_r_r_carry.regA.GetRegID());
	WReg regB = gpReg<WReg>(imlInstruction->op_r_r_r_carry.regB.GetRegID());
	WReg regCarry = gpReg<WReg>(imlInstruction->op_r_r_r_carry.regCarry.GetRegID());

	if (imlInstruction->operation == PPCREC_IML_OP_ADD)
	{
		adds(regR, regA, regB);
		cset(regCarry, Cond::CS);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_ADD_WITH_CARRY)
	{
		adds(TEMP_GPR_1_WREG, regB, regCarry);
		adcs(regR, regA, TEMP_GPR_1_WREG);
		cset(regCarry, Cond::CS);
	}
	else
	{
		cemu_assert_suspicious();
		return false;
	}

	return true;
}

Cond ImlCondToArm64Cond(IMLCondition condition)
{
	switch (condition)
	{
	case IMLCondition::EQ:
		return Cond::EQ;
	case IMLCondition::NEQ:
		return Cond::NE;
	case IMLCondition::UNSIGNED_GT:
		return Cond::HI;
	case IMLCondition::UNSIGNED_LT:
		return Cond::LO;
	case IMLCondition::SIGNED_GT:
		return Cond::GT;
	case IMLCondition::SIGNED_LT:
		return Cond::LT;
	default:
	{
		cemu_assert_suspicious();
		return Cond::EQ;
	}
	}
}

void AArch64GenContext_t::compare(IMLInstruction* imlInstruction)
{
	WReg regR = gpReg<WReg>(imlInstruction->op_compare.regR.GetRegID());
	WReg regA = gpReg<WReg>(imlInstruction->op_compare.regA.GetRegID());
	WReg regB = gpReg<WReg>(imlInstruction->op_compare.regB.GetRegID());
	Cond cond = ImlCondToArm64Cond(imlInstruction->op_compare.cond);
	cmp(regA, regB);
	cset(regR, cond);
}

void AArch64GenContext_t::compare_s32(IMLInstruction* imlInstruction)
{
	WReg regR = gpReg<WReg>(imlInstruction->op_compare.regR.GetRegID());
	WReg regA = gpReg<WReg>(imlInstruction->op_compare.regA.GetRegID());
	sint32 imm = imlInstruction->op_compare_s32.immS32;
	auto cond = ImlCondToArm64Cond(imlInstruction->op_compare.cond);
	cmp_imm(regA, imm, TEMP_GPR_1_WREG);
	cset(regR, cond);
}

void AArch64GenContext_t::cjump(const std::unordered_map<IMLSegment*, Label>& labels, IMLInstruction* imlInstruction, IMLSegment* imlSegment)
{
	const Label& label = labels.at(imlSegment->nextSegmentBranchTaken);
	auto regBool = gpReg<WReg>(imlInstruction->op_conditional_jump.registerBool.GetRegID());
	Label skipJump;
	if (imlInstruction->op_conditional_jump.mustBeTrue)
		cbz(regBool, skipJump);
	else
		cbnz(regBool, skipJump);
	b(label);
	L(skipJump);
}

void AArch64GenContext_t::jump(const std::unordered_map<IMLSegment*, Label>& labels, IMLSegment* imlSegment)
{
	const Label& label = labels.at(imlSegment->nextSegmentBranchTaken);
	b(label);
}

void AArch64GenContext_t::conditionalJumpCycleCheck(const std::unordered_map<IMLSegment*, Label>& labels, IMLSegment* imlSegment)
{
	Label positiveRegCycles;
	const Label& label = labels.at(imlSegment->nextSegmentBranchTaken);
	Label skipJump;
	ldr(TEMP_GPR_1_WREG, AdrImm(HCPU_REG, offsetof(PPCInterpreter_t, remainingCycles)));
	tbz(TEMP_GPR_1_WREG, 31, skipJump);
	b(label);
	L(skipJump);
}

void ATTR_MS_ABI PPCRecompiler_getTBL(PPCInterpreter_t* ppcInterpreter, uint32 gprIndex)
{
	uint64 coreTime = coreinit::coreinit_getTimerTick();
	ppcInterpreter->gpr[gprIndex] = (uint32)(coreTime & 0xFFFFFFFF);
}

void ATTR_MS_ABI PPCRecompiler_getTBU(PPCInterpreter_t* ppcInterpreter, uint32 gprIndex)
{
	uint64 coreTime = coreinit::coreinit_getTimerTick();
	ppcInterpreter->gpr[gprIndex] = (uint32)((coreTime >> 32) & 0xFFFFFFFF);
}

void* ATTR_MS_ABI PPCRecompiler_virtualHLE(PPCInterpreter_t* ppcInterpreter, uint32 hleFuncId)
{
	void* prevRSPTemp = ppcInterpreter->rspTemp;
	if (hleFuncId == 0xFFD0)
	{
		ppcInterpreter->remainingCycles -= 500; // let subtract about 500 cycles for each HLE call
		ppcInterpreter->gpr[3] = 0;
		PPCInterpreter_nextInstruction(ppcInterpreter);
		return PPCInterpreter_getCurrentInstance();
	}
	else
	{
		auto hleCall = PPCInterpreter_getHLECall(hleFuncId);
		cemu_assert(hleCall != nullptr);
		hleCall(ppcInterpreter);
	}
	ppcInterpreter->rspTemp = prevRSPTemp;
	return PPCInterpreter_getCurrentInstance();
}

bool AArch64GenContext_t::macro(IMLInstruction* imlInstruction)
{
	if (imlInstruction->operation == PPCREC_IML_MACRO_B_TO_REG)
	{
		XReg branchDstReg = gpReg<XReg>(imlInstruction->op_macro.paramReg.GetRegID());

		mov(TEMP_GPR_1_XREG, offsetof(PPCRecompilerInstanceData_t, ppcRecompilerDirectJumpTable));
		add(TEMP_GPR_1_XREG, TEMP_GPR_1_XREG, branchDstReg, ShMod::LSL, 1);
		ldr(TEMP_GPR_1_XREG, AdrReg(PPC_REC_INSTANCE_REG, TEMP_GPR_1_XREG));
		mov(LR_XREG, branchDstReg);
		br(TEMP_GPR_1_XREG);
		return true;
	}
	else if (imlInstruction->operation == PPCREC_IML_MACRO_BL)
	{
		uint32 newLR = imlInstruction->op_macro.param + 4;

		mov(TEMP_GPR_1_WREG, newLR);
		str(TEMP_GPR_1_WREG, AdrImm(HCPU_REG, offsetof(PPCInterpreter_t, spr.LR)));

		uint32 newIP = imlInstruction->op_macro.param2;
		uint64 lookupOffset = (uint64)offsetof(PPCRecompilerInstanceData_t, ppcRecompilerDirectJumpTable) + (uint64)newIP * 2ULL;
		mov(TEMP_GPR_1_XREG, lookupOffset);
		ldr(TEMP_GPR_1_XREG, AdrReg(PPC_REC_INSTANCE_REG, TEMP_GPR_1_XREG));
		mov(LR_WREG, newIP);
		br(TEMP_GPR_1_XREG);
		return true;
	}
	else if (imlInstruction->operation == PPCREC_IML_MACRO_B_FAR)
	{
		uint32 newIP = imlInstruction->op_macro.param2;
		uint64 lookupOffset = (uint64)offsetof(PPCRecompilerInstanceData_t, ppcRecompilerDirectJumpTable) + (uint64)newIP * 2ULL;
		mov(TEMP_GPR_1_XREG, lookupOffset);
		ldr(TEMP_GPR_1_XREG, AdrReg(PPC_REC_INSTANCE_REG, TEMP_GPR_1_XREG));
		mov(LR_WREG, newIP);
		br(TEMP_GPR_1_XREG);
		return true;
	}
	else if (imlInstruction->operation == PPCREC_IML_MACRO_LEAVE)
	{
		uint32 currentInstructionAddress = imlInstruction->op_macro.param;
		mov(TEMP_GPR_1_XREG, (uint64)offsetof(PPCRecompilerInstanceData_t, ppcRecompilerDirectJumpTable)); // newIP = 0 special value for recompiler exit
		ldr(TEMP_GPR_1_XREG, AdrReg(PPC_REC_INSTANCE_REG, TEMP_GPR_1_XREG));
		mov(LR_WREG, currentInstructionAddress);
		br(TEMP_GPR_1_XREG);
		return true;
	}
	else if (imlInstruction->operation == PPCREC_IML_MACRO_DEBUGBREAK)
	{
		return true;
	}
	else if (imlInstruction->operation == PPCREC_IML_MACRO_COUNT_CYCLES)
	{
		uint32 cycleCount = imlInstruction->op_macro.param;
		AdrImm adrCycles = AdrImm(HCPU_REG, offsetof(PPCInterpreter_t, remainingCycles));
		ldr(TEMP_GPR_1_WREG, adrCycles);
		sub_imm(TEMP_GPR_1_WREG, TEMP_GPR_1_WREG, cycleCount, TEMP_GPR_2_WREG);
		str(TEMP_GPR_1_WREG, adrCycles);
		return true;
	}
	else if (imlInstruction->operation == PPCREC_IML_MACRO_HLE)
	{
		uint32 ppcAddress = imlInstruction->op_macro.param;
		uint32 funcId = imlInstruction->op_macro.param2;
		Label cyclesLeftLabel;

		// update instruction pointer
		mov(TEMP_GPR_1_WREG, ppcAddress);
		str(TEMP_GPR_1_WREG, AdrImm(HCPU_REG, offsetof(PPCInterpreter_t, instructionPointer)));
		// set parameters
		str(x30, AdrPreImm(sp, -16));

		mov(x0, HCPU_REG);
		mov(w1, funcId);
		// call HLE function

		mov(TEMP_GPR_1_XREG, (uint64)PPCRecompiler_virtualHLE);
		blr(TEMP_GPR_1_XREG);

		mov(HCPU_REG, x0);

		ldr(x30, AdrPostImm(sp, 16));

		// check if cycles where decreased beyond zero, if yes -> leave recompiler
		ldr(TEMP_GPR_1_WREG, AdrImm(HCPU_REG, offsetof(PPCInterpreter_t, remainingCycles)));
		tbz(TEMP_GPR_1_WREG, 31, cyclesLeftLabel); // check if negative

		mov(TEMP_GPR_1_XREG, offsetof(PPCRecompilerInstanceData_t, ppcRecompilerDirectJumpTable));
		ldr(TEMP_GPR_1_XREG, AdrReg(PPC_REC_INSTANCE_REG, TEMP_GPR_1_XREG));
		ldr(LR_WREG, AdrImm(HCPU_REG, offsetof(PPCInterpreter_t, instructionPointer)));
		// JMP [recompilerCallTable+EAX/4*8]
		br(TEMP_GPR_1_XREG);

		L(cyclesLeftLabel);
		// check if instruction pointer was changed
		// assign new instruction pointer to EAX
		ldr(LR_WREG, AdrImm(HCPU_REG, offsetof(PPCInterpreter_t, instructionPointer)));
		mov(TEMP_GPR_1_XREG, offsetof(PPCRecompilerInstanceData_t, ppcRecompilerDirectJumpTable));
		// remember instruction pointer in REG_EDX
		// EAX *= 2
		add(TEMP_GPR_1_XREG, TEMP_GPR_1_XREG, LR_XREG, ShMod::LSL, 1);
		// ADD RAX, R15 (R15 -> Pointer to ppcRecompilerInstanceData
		ldr(TEMP_GPR_1_XREG, AdrReg(PPC_REC_INSTANCE_REG, TEMP_GPR_1_XREG));
		// JMP [ppcRecompilerDirectJumpTable+RAX/4*8]
		br(TEMP_GPR_1_XREG);
		return true;
	}
	else if (imlInstruction->operation == PPCREC_IML_MACRO_MFTB)
	{
		uint32 ppcAddress = imlInstruction->op_macro.param;
		uint32 sprId = imlInstruction->op_macro.param2 & 0xFFFF;
		uint32 gprIndex = (imlInstruction->op_macro.param2 >> 16) & 0x1F;

		// update instruction pointer
		mov(TEMP_GPR_1_WREG, ppcAddress);
		str(TEMP_GPR_1_WREG, AdrImm(HCPU_REG, offsetof(PPCInterpreter_t, instructionPointer)));
		// set parameters

		mov(x0, HCPU_REG);
		mov(x1, gprIndex);
		// call function
		if (sprId == SPR_TBL)
			mov(TEMP_GPR_1_XREG, (uint64)PPCRecompiler_getTBL);
		else if (sprId == SPR_TBU)
			mov(TEMP_GPR_1_XREG, (uint64)PPCRecompiler_getTBU);
		else
			cemu_assert_suspicious();

		str(x30, AdrPreImm(sp, -16));
		blr(TEMP_GPR_1_XREG);
		ldr(x30, AdrPostImm(sp, 16));
		return true;
	}
	else
	{
		cemuLog_log(LogType::Recompiler, "Unknown recompiler macro operation %d\n", imlInstruction->operation);
		cemu_assert_suspicious();
	}
	return false;
}

bool AArch64GenContext_t::load(IMLInstruction* imlInstruction, bool indexed)
{
	cemu_assert_debug(imlInstruction->op_storeLoad.registerData.GetRegFormat() == IMLRegFormat::I32);
	cemu_assert_debug(imlInstruction->op_storeLoad.registerMem.GetRegFormat() == IMLRegFormat::I32);
	if (indexed)
		cemu_assert_debug(imlInstruction->op_storeLoad.registerMem2.GetRegFormat() == IMLRegFormat::I32);

	sint32 memOffset = imlInstruction->op_storeLoad.immS32;
	bool signExtend = imlInstruction->op_storeLoad.flags2.signExtend;
	bool switchEndian = imlInstruction->op_storeLoad.flags2.swapEndian;
	WReg memReg = gpReg<WReg>(imlInstruction->op_storeLoad.registerMem.GetRegID());
	WReg dataReg = gpReg<WReg>(imlInstruction->op_storeLoad.registerData.GetRegID());

	add_imm(TEMP_GPR_1_WREG, memReg, memOffset, TEMP_GPR_1_WREG);
	if (indexed)
		add(TEMP_GPR_1_WREG, TEMP_GPR_1_WREG, gpReg<WReg>(imlInstruction->op_storeLoad.registerMem2.GetRegID()));

	auto adr = AdrExt(MEM_BASE_REG, TEMP_GPR_1_WREG, ExtMod::UXTW);
	if (imlInstruction->op_storeLoad.copyWidth == 32)
	{
		ldr(dataReg, adr);
		if (switchEndian)
			rev(dataReg, dataReg);
	}
	else if (imlInstruction->op_storeLoad.copyWidth == 16)
	{
		if (switchEndian)
		{
			ldrh(dataReg, adr);
			rev(dataReg, dataReg);
			if (signExtend)
				asr(dataReg, dataReg, 16);
			else
				lsr(dataReg, dataReg, 16);
		}
		else
		{
			if (signExtend)
				ldrsh(dataReg, adr);
			else
				ldrh(dataReg, adr);
		}
	}
	else if (imlInstruction->op_storeLoad.copyWidth == 8)
	{
		if (signExtend)
			ldrsb(dataReg, adr);
		else
			ldrb(dataReg, adr);
	}
	else
	{
		return false;
	}
	return true;
}

bool AArch64GenContext_t::store(IMLInstruction* imlInstruction, bool indexed)
{
	cemu_assert_debug(imlInstruction->op_storeLoad.registerData.GetRegFormat() == IMLRegFormat::I32);
	cemu_assert_debug(imlInstruction->op_storeLoad.registerMem.GetRegFormat() == IMLRegFormat::I32);
	if (indexed)
		cemu_assert_debug(imlInstruction->op_storeLoad.registerMem2.GetRegFormat() == IMLRegFormat::I32);

	WReg dataReg = gpReg<WReg>(imlInstruction->op_storeLoad.registerData.GetRegID());
	WReg memReg = gpReg<WReg>(imlInstruction->op_storeLoad.registerMem.GetRegID());
	sint32 memOffset = imlInstruction->op_storeLoad.immS32;
	bool swapEndian = imlInstruction->op_storeLoad.flags2.swapEndian;

	add_imm(TEMP_GPR_1_WREG, memReg, memOffset, TEMP_GPR_1_WREG);
	if (indexed)
		add(TEMP_GPR_1_WREG, TEMP_GPR_1_WREG, gpReg<WReg>(imlInstruction->op_storeLoad.registerMem2.GetRegID()));
	AdrExt adr = AdrExt(MEM_BASE_REG, TEMP_GPR_1_WREG, ExtMod::UXTW);
	if (imlInstruction->op_storeLoad.copyWidth == 32)
	{
		if (swapEndian)
		{
			rev(TEMP_GPR_2_WREG, dataReg);
			str(TEMP_GPR_2_WREG, adr);
		}
		else
		{
			str(dataReg, adr);
		}
	}
	else if (imlInstruction->op_storeLoad.copyWidth == 16)
	{
		if (swapEndian)
		{
			rev16(TEMP_GPR_2_WREG, dataReg);
			strh(TEMP_GPR_2_WREG, adr);
		}
		else
		{
			strh(dataReg, adr);
		}
	}
	else if (imlInstruction->op_storeLoad.copyWidth == 8)
	{
		strb(dataReg, adr);
	}
	else
	{
		return false;
	}
	return true;
}

void AArch64GenContext_t::atomic_cmp_store(IMLInstruction* imlInstruction)
{
	WReg outReg = gpReg<WReg>(imlInstruction->op_atomic_compare_store.regBoolOut.GetRegID());
	WReg eaReg = gpReg<WReg>(imlInstruction->op_atomic_compare_store.regEA.GetRegID());
	WReg valReg = gpReg<WReg>(imlInstruction->op_atomic_compare_store.regWriteValue.GetRegID());
	WReg cmpValReg = gpReg<WReg>(imlInstruction->op_atomic_compare_store.regCompareValue.GetRegID());

	if (s_cpu.isAtomicSupported())
	{
		mov(TEMP_GPR_2_WREG, cmpValReg);
		add(TEMP_GPR_1_XREG, MEM_BASE_REG, eaReg, ExtMod::UXTW);
		casal(TEMP_GPR_2_WREG, valReg, AdrNoOfs(TEMP_GPR_1_XREG));
		cmp(TEMP_GPR_2_WREG, cmpValReg);
		cset(outReg, Cond::EQ);
	}
	else
	{
		Label endCmpStore;
		Label notEqual;
		Label storeFailed;

		add(TEMP_GPR_1_XREG, MEM_BASE_REG, eaReg, ExtMod::UXTW);
		L(storeFailed);
		ldaxr(TEMP_GPR_2_WREG, AdrNoOfs(TEMP_GPR_1_XREG));
		cmp(TEMP_GPR_2_WREG, cmpValReg);
		bne(notEqual);
		stlxr(TEMP_GPR_2_WREG, valReg, AdrNoOfs(TEMP_GPR_1_XREG));
		cbnz(TEMP_GPR_2_WREG, storeFailed);
		mov(outReg, 1);
		b(endCmpStore);

		L(notEqual);
		mov(outReg, 0);
		L(endCmpStore);
	}
}

void AArch64GenContext_t::gqr_generateScaleCode(VReg& dataReg, bool isLoad, bool scalePS1, const IMLReg& registerGQR)
{
	auto gqrReg = gpReg<XReg>(registerGQR.GetRegID());
	// load GQR
	mov(TEMP_GPR_1_XREG, gqrReg);
	// extract scale field and multiply by 16 to get array offset
	lsr(TEMP_GPR_1_WREG, TEMP_GPR_1_WREG, (isLoad ? 16 : 0) + 8 - 4);
	and_(TEMP_GPR_1_WREG, TEMP_GPR_1_WREG, (0x3F << 4));
	// multiply dataReg by scale
	add(TEMP_GPR_1_XREG, TEMP_GPR_1_XREG, PPC_REC_INSTANCE_REG);
	if (isLoad)
	{
		if (scalePS1)
			mov(TEMP_GPR_2_XREG, offsetof(PPCRecompilerInstanceData_t, _psq_ld_scale_ps0_ps1));
		else
			mov(TEMP_GPR_2_XREG, offsetof(PPCRecompilerInstanceData_t, _psq_ld_scale_ps0_1));
	}
	else
	{
		if (scalePS1)
			mov(TEMP_GPR_2_XREG, offsetof(PPCRecompilerInstanceData_t, _psq_st_scale_ps0_ps1));
		else
			mov(TEMP_GPR_2_XREG, offsetof(PPCRecompilerInstanceData_t, _psq_st_scale_ps0_1));
	}
	add(TEMP_GPR_1_XREG, TEMP_GPR_1_XREG, TEMP_GPR_2_XREG);
	ld1(TEMP_FPR_2_VREG.d2, AdrNoOfs(TEMP_GPR_1_XREG));
	fmul(dataReg.d2, dataReg.d2, TEMP_FPR_2_VREG.d2);
}

// generate code for PSQ load for a particular type
// if scaleGQR is -1 then a scale of 1.0 is assumed (no scale)
void AArch64GenContext_t::psq_load(uint8 mode, VReg& dataReg, WReg& memReg, WReg& indexReg, sint32 memImmS32, bool indexed, const IMLReg& registerGQR)
{
	if (mode == PPCREC_FPR_LD_MODE_PSQ_FLOAT_PS0_PS1)
	{
		add_imm(TEMP_GPR_1_WREG, memReg, memImmS32, TEMP_GPR_1_WREG);
		if (indexed)
			cemu_assert_suspicious();
		ldr(DReg(dataReg.getIdx()), AdrExt(MEM_BASE_REG, TEMP_GPR_1_WREG, ExtMod::UXTW));
		rev32(dataReg.b16, dataReg.b16);
		fcvtl(dataReg.d2, dataReg.s2);
		// note: floats are not scaled
	}
	else if (mode == PPCREC_FPR_LD_MODE_PSQ_FLOAT_PS0)
	{
		add_imm(TEMP_GPR_1_WREG, memReg, memImmS32, TEMP_GPR_1_WREG);
		if (indexed)
			cemu_assert_suspicious();
		ldr(SReg(dataReg.getIdx()), AdrExt(MEM_BASE_REG, TEMP_GPR_1_WREG, ExtMod::UXTW));
		rev32(dataReg.b8, dataReg.b8);
		fcvtl(dataReg.d2, dataReg.s2);
		// load constant 1.0 to temp register
		mov(TEMP_GPR_1_XREG, DOUBLE_1_0);
		// overwrite lower half with single from memory
		mov(dataReg.d[1], TEMP_GPR_1_XREG);
		// note: floats are not scaled
	}
	else
	{
		if (indexed)
			cemu_assert_suspicious();
		bool loadPS1 = false;
		add_imm(TEMP_GPR_1_WREG, memReg, memImmS32, TEMP_GPR_1_WREG);
		auto adr = AdrExt(MEM_BASE_REG, TEMP_GPR_1_WREG, ExtMod::UXTW);
		if (mode == PPCREC_FPR_LD_MODE_PSQ_S16_PS0_PS1 || mode == PPCREC_FPR_LD_MODE_PSQ_U16_PS0_PS1)
		{
			loadPS1 = true;
			ldr(SReg(dataReg.getIdx()), adr);
			rev16(dataReg.b16, dataReg.b16);
			if (mode == PPCREC_FPR_LD_MODE_PSQ_S16_PS0_PS1)
			{
				smov(TEMP_GPR_1_XREG, dataReg.h[1]);
				mov(dataReg.d[1], TEMP_GPR_1_XREG);
				smov(TEMP_GPR_1_XREG, dataReg.h[0]);
				mov(dataReg.d[0], TEMP_GPR_1_XREG);
				scvtf(dataReg.d2, dataReg.d2);
			}
			else
			{
				mov(dataReg.h[4], dataReg.h[1]);
				mov(dataReg.h[1], wzr);
				ucvtf(dataReg.d2, dataReg.d2);
			}
		}
		else if (mode == PPCREC_FPR_LD_MODE_PSQ_S16_PS0 || mode == PPCREC_FPR_LD_MODE_PSQ_U16_PS0)
		{
			add_imm(TEMP_GPR_1_WREG, memReg, memImmS32, TEMP_GPR_1_WREG);
			ldr(HReg(dataReg.getIdx()), adr);
			rev16(dataReg.b16, dataReg.b16);
			mov(TEMP_GPR_1_XREG, 1);
			mov(dataReg.d[1], TEMP_GPR_1_XREG);
			if (mode == PPCREC_FPR_LD_MODE_PSQ_S16_PS0)
			{
				smov(TEMP_GPR_1_XREG, dataReg.h[0]);
				mov(dataReg.d[0], TEMP_GPR_1_XREG);
				scvtf(dataReg.d2, dataReg.d2);
			}
			else
			{
				ucvtf(dataReg.d2, dataReg.d2);
			}
		}
		else if (mode == PPCREC_FPR_LD_MODE_PSQ_S8_PS0_PS1 || mode == PPCREC_FPR_LD_MODE_PSQ_U8_PS0_PS1)
		{
			loadPS1 = true;
			add_imm(TEMP_GPR_1_WREG, memReg, memImmS32, TEMP_GPR_1_WREG);
			ldr(HReg(dataReg.getIdx()), adr);
			if (mode == PPCREC_FPR_LD_MODE_PSQ_S8_PS0_PS1)
			{
				smov(TEMP_GPR_1_XREG, dataReg.b[1]);
				mov(dataReg.d[1], TEMP_GPR_1_XREG);
				smov(TEMP_GPR_1_XREG, dataReg.b[0]);
				mov(dataReg.d[0], TEMP_GPR_1_XREG);
				scvtf(dataReg.d2, dataReg.d2);
			}
			else
			{
				mov(dataReg.b[8], dataReg.b[1]);
				mov(dataReg.b[1], wzr);
				ucvtf(dataReg.d2, dataReg.d2);
			}
		}
		else if (mode == PPCREC_FPR_LD_MODE_PSQ_S8_PS0 || mode == PPCREC_FPR_LD_MODE_PSQ_U8_PS0)
		{
			add_imm(TEMP_GPR_1_WREG, memReg, memImmS32, TEMP_GPR_1_WREG);
			ldr(BReg(dataReg.getIdx()), adr);
			mov(TEMP_GPR_1_XREG, 1);
			mov(dataReg.d[1], TEMP_GPR_1_XREG);
			if (mode == PPCREC_FPR_LD_MODE_PSQ_S8_PS0)
			{
				smov(TEMP_GPR_1_XREG, dataReg.b[0]);
				mov(dataReg.d[0], TEMP_GPR_1_XREG);
				scvtf(dataReg.d2, dataReg.d2);
			}
			else
			{
				ucvtf(dataReg.d2, dataReg.d2);
			}
		}
		// scale
		if (registerGQR.IsValid())
			gqr_generateScaleCode(dataReg, true, loadPS1, registerGQR);
	}
}

void AArch64GenContext_t::psq_load_generic(uint8 mode, VReg& dataReg, WReg& memReg, WReg& indexReg, sint32 memImmS32, bool indexed, const IMLReg& registerGQR)
{
	bool loadPS1 = (mode == PPCREC_FPR_LD_MODE_PSQ_GENERIC_PS0_PS1);
	Label u8FormatLabel, u16FormatLabel, s8FormatLabel, s16FormatLabel, casesEndLabel;

	// load GQR & extract load type field
	lsr(TEMP_GPR_1_WREG, gpReg<WReg>(registerGQR.GetRegID()), 16);
	and_(TEMP_GPR_1_WREG, TEMP_GPR_1_WREG, 7);

	// jump cases
	cmp(TEMP_GPR_1_WREG, 4); // type 4 -> u8
	beq(u8FormatLabel);

	cmp(TEMP_GPR_1_WREG, 5); // type 5 -> u16
	beq(u16FormatLabel);

	cmp(TEMP_GPR_1_WREG, 6); // type 6 -> s8
	beq(s8FormatLabel);

	cmp(TEMP_GPR_1_WREG, 7); // type 7 -> s16
	beq(s16FormatLabel);

	// default case -> float

	// generate cases
	psq_load(loadPS1 ? PPCREC_FPR_LD_MODE_PSQ_FLOAT_PS0_PS1 : PPCREC_FPR_LD_MODE_PSQ_FLOAT_PS0, dataReg, memReg, indexReg, memImmS32, indexed, registerGQR);
	b(casesEndLabel);

	L(u16FormatLabel);
	psq_load(loadPS1 ? PPCREC_FPR_LD_MODE_PSQ_U16_PS0_PS1 : PPCREC_FPR_LD_MODE_PSQ_U16_PS0, dataReg, memReg, indexReg, memImmS32, indexed, registerGQR);
	b(casesEndLabel);

	L(s16FormatLabel);
	psq_load(loadPS1 ? PPCREC_FPR_LD_MODE_PSQ_S16_PS0_PS1 : PPCREC_FPR_LD_MODE_PSQ_S16_PS0, dataReg, memReg, indexReg, memImmS32, indexed, registerGQR);
	b(casesEndLabel);

	L(u8FormatLabel);
	psq_load(loadPS1 ? PPCREC_FPR_LD_MODE_PSQ_U8_PS0_PS1 : PPCREC_FPR_LD_MODE_PSQ_U8_PS0, dataReg, memReg, indexReg, memImmS32, indexed, registerGQR);
	b(casesEndLabel);

	L(s8FormatLabel);
	psq_load(loadPS1 ? PPCREC_FPR_LD_MODE_PSQ_S8_PS0_PS1 : PPCREC_FPR_LD_MODE_PSQ_S8_PS0, dataReg, memReg, indexReg, memImmS32, indexed, registerGQR);

	L(casesEndLabel);
}

bool AArch64GenContext_t::fpr_load(IMLInstruction* imlInstruction, bool indexed)
{
	IMLRegID dataRegId = imlInstruction->op_storeLoad.registerData.GetRegID();
	VReg dataVReg = fpReg<VReg>(imlInstruction->op_storeLoad.registerData.GetRegID());
	SReg dataSReg = fpReg<SReg>(dataRegId);
	WReg realRegisterMem = gpReg<WReg>(imlInstruction->op_storeLoad.registerMem.GetRegID());
	WReg realRegisterMem2 = indexed ? gpReg<WReg>(imlInstruction->op_storeLoad.registerMem2.GetRegID()) : wzr;
	sint32 adrOffset = imlInstruction->op_storeLoad.immS32;
	uint8 mode = imlInstruction->op_storeLoad.mode;

	if (mode == PPCREC_FPR_LD_MODE_SINGLE_INTO_PS0_PS1)
	{
		add_imm(TEMP_GPR_1_WREG, realRegisterMem, adrOffset, TEMP_GPR_1_WREG);
		if (indexed)
			add(TEMP_GPR_1_WREG, TEMP_GPR_1_WREG, realRegisterMem2);
		ldr(TEMP_GPR_1_WREG, AdrExt(MEM_BASE_REG, TEMP_GPR_1_WREG, ExtMod::UXTW));
		rev(TEMP_GPR_1_WREG, TEMP_GPR_1_WREG);
		fmov(dataSReg, TEMP_GPR_1_WREG);

		if (imlInstruction->op_storeLoad.flags2.notExpanded)
		{
			// leave value as single
		}
		else
		{
			fcvtl(dataVReg.d2, dataVReg.s2);
			mov(dataVReg.d[1], dataVReg.d[0]);
		}
	}
	else if (mode == PPCREC_FPR_LD_MODE_DOUBLE_INTO_PS0)
	{
		add_imm(TEMP_GPR_1_WREG, realRegisterMem, adrOffset, TEMP_GPR_1_WREG);
		if (indexed)
			add(TEMP_GPR_1_WREG, TEMP_GPR_1_WREG, realRegisterMem2);
		ldr(TEMP_GPR_1_XREG, AdrExt(MEM_BASE_REG, TEMP_GPR_1_WREG, ExtMod::UXTW));
		rev(TEMP_GPR_1_XREG, TEMP_GPR_1_XREG);
		mov(dataVReg.d[0], TEMP_GPR_1_XREG);
	}
	else if (mode == PPCREC_FPR_LD_MODE_PSQ_FLOAT_PS0_PS1 ||
			 mode == PPCREC_FPR_LD_MODE_PSQ_FLOAT_PS0 ||
			 mode == PPCREC_FPR_LD_MODE_PSQ_S16_PS0 ||
			 mode == PPCREC_FPR_LD_MODE_PSQ_S16_PS0_PS1 ||
			 mode == PPCREC_FPR_LD_MODE_PSQ_U16_PS0 ||
			 mode == PPCREC_FPR_LD_MODE_PSQ_U16_PS0_PS1 ||
			 mode == PPCREC_FPR_LD_MODE_PSQ_S8_PS0 ||
			 mode == PPCREC_FPR_LD_MODE_PSQ_S8_PS0_PS1 ||
			 mode == PPCREC_FPR_LD_MODE_PSQ_U8_PS0 ||
			 mode == PPCREC_FPR_LD_MODE_PSQ_U8_PS0_PS1)
	{
		psq_load(mode, dataVReg, realRegisterMem, realRegisterMem2, imlInstruction->op_storeLoad.immS32, indexed);
	}
	else if (mode == PPCREC_FPR_LD_MODE_PSQ_GENERIC_PS0_PS1 ||
			 mode == PPCREC_FPR_LD_MODE_PSQ_GENERIC_PS0)
	{
		psq_load_generic(mode, dataVReg, realRegisterMem, realRegisterMem2, imlInstruction->op_storeLoad.immS32, indexed, imlInstruction->op_storeLoad.registerGQR);
	}
	else
	{
		return false;
	}
	return true;
}

void AArch64GenContext_t::psq_store(uint8 mode, IMLRegID dataRegId, WReg& memReg, WReg& indexReg, sint32 memOffset, bool indexed, const IMLReg& registerGQR)
{
	auto dataVReg = fpReg<VReg>(dataRegId);
	auto dataDReg = fpReg<DReg>(dataRegId);

	bool storePS1 = (mode == PPCREC_FPR_ST_MODE_PSQ_FLOAT_PS0_PS1 ||
					 mode == PPCREC_FPR_ST_MODE_PSQ_S8_PS0_PS1 ||
					 mode == PPCREC_FPR_ST_MODE_PSQ_U8_PS0_PS1 ||
					 mode == PPCREC_FPR_ST_MODE_PSQ_U16_PS0_PS1 ||
					 mode == PPCREC_FPR_ST_MODE_PSQ_S16_PS0_PS1);
	bool isFloat = mode == PPCREC_FPR_ST_MODE_PSQ_FLOAT_PS0 || mode == PPCREC_FPR_ST_MODE_PSQ_FLOAT_PS0_PS1;

	if (registerGQR.IsValid())
	{
		// move to temporary reg and update data reg
		mov(TEMP_FPR_1_VREG.b16, dataVReg.b16);
		dataVReg = TEMP_FPR_1_VREG;
		// apply scale
		if (!isFloat)
			gqr_generateScaleCode(dataVReg, false, storePS1, registerGQR);
	}
	if (mode == PPCREC_FPR_ST_MODE_PSQ_FLOAT_PS0)
	{
		add_imm(TEMP_GPR_1_WREG, memReg, memOffset, TEMP_GPR_1_WREG);
		if (indexed)
			add(TEMP_GPR_1_WREG, TEMP_GPR_1_WREG, indexReg);
		fcvt(TEMP_FPR_1_SREG, dataDReg);
		rev32(TEMP_FPR_1_VREG.b8, TEMP_FPR_1_VREG.b8);
		str(TEMP_FPR_1_SREG, AdrExt(MEM_BASE_REG, TEMP_GPR_1_WREG, ExtMod::UXTW));
	}
	else if (mode == PPCREC_FPR_ST_MODE_PSQ_FLOAT_PS0_PS1)
	{
		add_imm(TEMP_GPR_1_WREG, memReg, memOffset, TEMP_GPR_1_WREG);
		if (indexed)
			add(TEMP_GPR_1_WREG, TEMP_GPR_1_WREG, indexReg);
		fcvtn(TEMP_FPR_1_VREG.s2, dataVReg.d2);
		rev32(TEMP_FPR_1_VREG.b8, TEMP_FPR_1_VREG.b8);
		str(TEMP_FPR_1_DREG, AdrExt(MEM_BASE_REG, TEMP_GPR_1_WREG, ExtMod::UXTW));
	}
	else
	{
		// store as integer
		if (indexed)
			cemu_assert_suspicious(); // unsupported

		if (mode == PPCREC_FPR_ST_MODE_PSQ_U8_PS0 || mode == PPCREC_FPR_ST_MODE_PSQ_U16_PS0)
		{
			fcvtzs(TEMP_GPR_1_WREG, dataDReg);
			uint64 maxVal = mode == PPCREC_FPR_ST_MODE_PSQ_U8_PS0 ? 255 : 65535;
			// clamp
			mov(TEMP_GPR_2_WREG, maxVal);
			bic(TEMP_GPR_1_WREG, TEMP_GPR_1_WREG, TEMP_GPR_1_WREG, ShMod::ASR, 31);
			cmp(TEMP_GPR_1_WREG, TEMP_GPR_2_WREG);
			csel(TEMP_GPR_2_WREG, TEMP_GPR_1_WREG, TEMP_GPR_2_WREG, Cond::LT);
			// write to memory
			add_imm(TEMP_GPR_1_WREG, memReg, memOffset, TEMP_GPR_1_WREG);
			auto adr = AdrExt(MEM_BASE_REG, TEMP_GPR_1_WREG, ExtMod::UXTW);
			if (mode == PPCREC_FPR_ST_MODE_PSQ_U8_PS0)
			{
				strb(TEMP_GPR_2_WREG, AdrExt(MEM_BASE_REG, TEMP_GPR_1_WREG, ExtMod::UXTW));
			}
			else
			{
				rev16(TEMP_GPR_2_WREG, TEMP_GPR_2_WREG);
				strh(TEMP_GPR_2_WREG, AdrExt(MEM_BASE_REG, TEMP_GPR_1_WREG, ExtMod::UXTW));
			}
		}
		else if (mode == PPCREC_FPR_ST_MODE_PSQ_S8_PS0 || mode == PPCREC_FPR_ST_MODE_PSQ_S16_PS0)
		{
			fcvtzs(TEMP_GPR_1_WREG, dataDReg);
			sint32 maxVal;
			sint32 minVal;
			if (mode == PPCREC_FPR_ST_MODE_PSQ_S8_PS0)
			{
				maxVal = 127;
				minVal = -128;
			}
			else
			{
				maxVal = 32767;
				minVal = -32768;
			}
			// clamp
			mov(TEMP_GPR_2_WREG, minVal);
			cmp(TEMP_GPR_1_WREG, TEMP_GPR_2_WREG);
			csel(TEMP_GPR_2_WREG, TEMP_GPR_1_WREG, TEMP_GPR_2_WREG, Cond::GT);
			mov(TEMP_GPR_1_WREG, maxVal);
			cmp(TEMP_GPR_2_WREG, TEMP_GPR_1_WREG);
			csel(TEMP_GPR_2_WREG, TEMP_GPR_2_WREG, TEMP_GPR_1_WREG, Cond::LT);
			add_imm(TEMP_GPR_1_WREG, memReg, memOffset, TEMP_GPR_1_WREG);
			auto adr = AdrExt(MEM_BASE_REG, TEMP_GPR_1_WREG, ExtMod::UXTW);
			// write to memory
			if (mode == PPCREC_FPR_ST_MODE_PSQ_S8_PS0)
			{
				strb(TEMP_GPR_2_WREG, adr);
			}
			else
			{
				rev16(TEMP_GPR_2_WREG, TEMP_GPR_2_WREG);
				strh(TEMP_GPR_2_WREG, adr);
			}
		}
		else if (mode == PPCREC_FPR_ST_MODE_PSQ_S8_PS0_PS1 || mode == PPCREC_FPR_ST_MODE_PSQ_U8_PS0_PS1)
		{
			fcvtzs(TEMP_FPR_1_VREG.d2, dataVReg.d2);
			xtn(TEMP_FPR_1_VREG.s2, TEMP_FPR_1_VREG.d2);
			// clamp
			if (mode == PPCREC_FPR_ST_MODE_PSQ_S8_PS0_PS1)
				movi(TEMP_FPR_2_VREG.s2, 127);
			else
				movi(TEMP_FPR_2_DREG, (255UL << 32 | 255UL));
			smin(TEMP_FPR_1_VREG.s2, TEMP_FPR_1_VREG.s2, TEMP_FPR_2_VREG.s2);
			if (mode == PPCREC_FPR_ST_MODE_PSQ_S8_PS0_PS1)
				mvni(TEMP_FPR_2_VREG.s2, 127);
			else
				movi(TEMP_FPR_2_DREG, 0);
			smax(TEMP_FPR_1_VREG.s2, TEMP_FPR_1_VREG.s2, TEMP_FPR_2_VREG.s2);
			mov(TEMP_FPR_1_VREG.b[1], TEMP_FPR_1_VREG.b[4]);
			// write to memory
			add_imm(TEMP_GPR_1_WREG, memReg, memOffset, TEMP_GPR_1_WREG);
			str(TEMP_FPR_1_HREG, AdrExt(MEM_BASE_REG, TEMP_GPR_1_WREG, ExtMod::UXTW));
		}
		else if (mode == PPCREC_FPR_ST_MODE_PSQ_S16_PS0_PS1 || mode == PPCREC_FPR_ST_MODE_PSQ_U16_PS0_PS1)
		{
			fcvtzs(TEMP_FPR_1_VREG.d2, dataVReg.d2);
			xtn(TEMP_FPR_1_VREG.s2, TEMP_FPR_1_VREG.d2);
			// clamp
			if (mode == PPCREC_FPR_ST_MODE_PSQ_S16_PS0_PS1)
				movi(TEMP_FPR_2_VREG.s2, 127, ShMod::MSL, 8); // Load 32767 in temp2VReg.2s
			else
				movi(TEMP_FPR_2_DREG, (65535UL << 32) | 65535UL);
			smin(TEMP_FPR_1_VREG.s2, TEMP_FPR_1_VREG.s2, TEMP_FPR_2_VREG.s2);
			if (mode == PPCREC_FPR_ST_MODE_PSQ_S16_PS0_PS1)
				mvni(TEMP_FPR_2_VREG.s2, 127, ShMod::MSL, 8); // Load -32768 in temp2VReg.2s
			else
				movi(TEMP_FPR_2_DREG, 0);
			smax(TEMP_FPR_1_VREG.s2, TEMP_FPR_1_VREG.s2, TEMP_FPR_2_VREG.s2);
			mov(TEMP_FPR_1_VREG.h[1], TEMP_FPR_1_VREG.h[2]);
			// endian swap
			rev16(TEMP_FPR_1_VREG.b8, TEMP_FPR_1_VREG.b8);
			// write to memory
			add_imm(TEMP_GPR_1_WREG, memReg, memOffset, TEMP_GPR_1_WREG);
			str(TEMP_FPR_1_SREG, AdrExt(MEM_BASE_REG, TEMP_GPR_1_WREG, ExtMod::UXTW));
		}
		else
		{
			cemu_assert_suspicious();
			return;
		}
	}
}

void AArch64GenContext_t::psq_store_generic(uint8 mode, IMLRegID dataRegId, WReg& memReg, WReg& indexReg, sint32 memOffset, bool indexed, const IMLReg& registerGQR)
{
	bool storePS1 = (mode == PPCREC_FPR_ST_MODE_PSQ_GENERIC_PS0_PS1);
	Label u8FormatLabel, u16FormatLabel, s8FormatLabel, s16FormatLabel, casesEndLabel;
	// load GQR & extract store type field
	and_(TEMP_GPR_1_WREG, gpReg<WReg>(registerGQR.GetRegID()), 7);

	// jump cases
	cmp(TEMP_GPR_1_WREG, 4); // type 4 -> u8
	beq(u8FormatLabel);

	cmp(TEMP_GPR_1_WREG, 5); // type 5 -> u16
	beq(u16FormatLabel);

	cmp(TEMP_GPR_1_WREG, 6); // type 6 -> s8
	beq(s8FormatLabel);

	cmp(TEMP_GPR_1_WREG, 7); // type 7 -> s16
	beq(s16FormatLabel);

	// default case -> float

	// generate cases
	psq_store(storePS1 ? PPCREC_FPR_ST_MODE_PSQ_FLOAT_PS0_PS1 : PPCREC_FPR_ST_MODE_PSQ_FLOAT_PS0, dataRegId, memReg, indexReg, memOffset, indexed, registerGQR);
	b(casesEndLabel);

	L(u16FormatLabel);
	psq_store(storePS1 ? PPCREC_FPR_ST_MODE_PSQ_U16_PS0_PS1 : PPCREC_FPR_ST_MODE_PSQ_U16_PS0, dataRegId, memReg, indexReg, memOffset, indexed, registerGQR);
	b(casesEndLabel);

	L(s16FormatLabel);
	psq_store(storePS1 ? PPCREC_FPR_ST_MODE_PSQ_S16_PS0_PS1 : PPCREC_FPR_ST_MODE_PSQ_S16_PS0, dataRegId, memReg, indexReg, memOffset, indexed, registerGQR);
	b(casesEndLabel);

	L(u8FormatLabel);
	psq_store(storePS1 ? PPCREC_FPR_ST_MODE_PSQ_U8_PS0_PS1 : PPCREC_FPR_ST_MODE_PSQ_U8_PS0, dataRegId, memReg, indexReg, memOffset, indexed, registerGQR);
	b(casesEndLabel);

	L(s8FormatLabel);
	psq_store(storePS1 ? PPCREC_FPR_ST_MODE_PSQ_S8_PS0_PS1 : PPCREC_FPR_ST_MODE_PSQ_S8_PS0, dataRegId, memReg, indexReg, memOffset, indexed, registerGQR);

	L(casesEndLabel);
}

// store to memory
bool AArch64GenContext_t::fpr_store(IMLInstruction* imlInstruction, bool indexed)
{
	IMLRegID dataRegId = imlInstruction->op_storeLoad.registerData.GetRegID();
	VReg dataReg = fpReg<VReg>(dataRegId);
	DReg dataDReg = fpReg<DReg>(dataRegId);
	WReg memReg = gpReg<WReg>(imlInstruction->op_storeLoad.registerMem.GetRegID());
	WReg indexReg = indexed ? gpReg<WReg>(imlInstruction->op_storeLoad.registerMem2.GetRegID()) : wzr;
	sint32 memOffset = imlInstruction->op_storeLoad.immS32;
	uint8 mode = imlInstruction->op_storeLoad.mode;

	if (mode == PPCREC_FPR_ST_MODE_SINGLE_FROM_PS0)
	{
		add_imm(TEMP_GPR_1_WREG, memReg, memOffset, TEMP_GPR_1_WREG);
		if (indexed)
			add(TEMP_GPR_1_WREG, TEMP_GPR_1_WREG, indexReg);
		auto adr = AdrExt(MEM_BASE_REG, TEMP_GPR_1_WREG, ExtMod::UXTW);
		if (imlInstruction->op_storeLoad.flags2.notExpanded)
		{
			// value is already in single format
			mov(TEMP_GPR_2_WREG, dataReg.s[0]);
		}
		else
		{
			fcvt(TEMP_FPR_1_SREG, dataDReg);
			fmov(TEMP_GPR_2_WREG, TEMP_FPR_1_SREG);
		}
		rev(TEMP_GPR_2_WREG, TEMP_GPR_2_WREG);
		str(TEMP_GPR_2_WREG, adr);
	}
	else if (mode == PPCREC_FPR_ST_MODE_DOUBLE_FROM_PS0)
	{
		add_imm(TEMP_GPR_1_WREG, memReg, memOffset, TEMP_GPR_1_WREG);
		if (indexed)
			add(TEMP_GPR_1_WREG, TEMP_GPR_1_WREG, indexReg);
		mov(TEMP_GPR_2_XREG, dataReg.d[0]);
		rev(TEMP_GPR_2_XREG, TEMP_GPR_2_XREG);
		str(TEMP_GPR_2_XREG, AdrExt(MEM_BASE_REG, TEMP_GPR_1_WREG, ExtMod::UXTW));
	}
	else if (mode == PPCREC_FPR_ST_MODE_UI32_FROM_PS0)
	{
		add_imm(TEMP_GPR_1_WREG, memReg, memOffset, TEMP_GPR_1_WREG);
		if (indexed)
			add(TEMP_GPR_1_WREG, TEMP_GPR_1_WREG, indexReg);
		mov(TEMP_GPR_2_WREG, dataReg.s[0]);
		rev(TEMP_GPR_2_WREG, TEMP_GPR_2_WREG);
		str(TEMP_GPR_2_WREG, AdrExt(MEM_BASE_REG, TEMP_GPR_1_WREG, ExtMod::UXTW));
	}
	else if (mode == PPCREC_FPR_ST_MODE_PSQ_FLOAT_PS0_PS1 ||
			 mode == PPCREC_FPR_ST_MODE_PSQ_FLOAT_PS0 ||
			 mode == PPCREC_FPR_ST_MODE_PSQ_S8_PS0 ||
			 mode == PPCREC_FPR_ST_MODE_PSQ_S8_PS0_PS1 ||
			 mode == PPCREC_FPR_ST_MODE_PSQ_U8_PS0 ||
			 mode == PPCREC_FPR_ST_MODE_PSQ_U8_PS0_PS1 ||
			 mode == PPCREC_FPR_ST_MODE_PSQ_S16_PS0 ||
			 mode == PPCREC_FPR_ST_MODE_PSQ_S16_PS0_PS1 ||
			 mode == PPCREC_FPR_ST_MODE_PSQ_U16_PS0 ||
			 mode == PPCREC_FPR_ST_MODE_PSQ_U16_PS0_PS1)
	{
		cemu_assert_debug(imlInstruction->op_storeLoad.flags2.notExpanded == false);
		psq_store(mode, dataRegId, memReg, indexReg, imlInstruction->op_storeLoad.immS32, indexed);
	}
	else if (mode == PPCREC_FPR_ST_MODE_PSQ_GENERIC_PS0_PS1 ||
			 mode == PPCREC_FPR_ST_MODE_PSQ_GENERIC_PS0)
	{
		psq_store_generic(mode, dataRegId, memReg, indexReg, imlInstruction->op_storeLoad.immS32, indexed, imlInstruction->op_storeLoad.registerGQR);
	}
	else
	{
		cemu_assert_suspicious();
		cemuLog_log(LogType::Recompiler, "PPCRecompilerAArch64Gen_imlInstruction_fpr_store(): Unsupported mode %d\n", mode);
		return false;
	}
	return true;
}

// FPR op FPR
void AArch64GenContext_t::fpr_r_r(IMLInstruction* imlInstruction)
{
	IMLRegID regAId = imlInstruction->op_fpr_r_r.regA.GetRegID();
	IMLRegID regRId = imlInstruction->op_fpr_r_r.regR.GetRegID();
	VReg regRVReg = fpReg<VReg>(regRId);
	VReg regAVReg = fpReg<VReg>(regAId);
	DReg regADReg = fpReg<DReg>(regAId);

	if (imlInstruction->operation == PPCREC_IML_OP_FPR_COPY_BOTTOM_TO_BOTTOM_AND_TOP)
	{
		dup(regRVReg.d2, regAVReg.d[0]);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_FPR_COPY_TOP_TO_BOTTOM_AND_TOP)
	{
		dup(regRVReg.d2, regAVReg.d[1]);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_FPR_COPY_BOTTOM_TO_BOTTOM)
	{
		if (regRId != regAId)
			mov(regRVReg.d[0], regAVReg.d[0]);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_FPR_COPY_BOTTOM_TO_TOP)
	{
		mov(regRVReg.d[1], regAVReg.d[0]);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_FPR_COPY_BOTTOM_AND_TOP_SWAPPED)
	{
		ext(regRVReg.b16, regAVReg.b16, regAVReg.b16, 8);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_FPR_COPY_TOP_TO_TOP)
	{
		if (regRId != regAId)
			mov(regRVReg.d[1], regAVReg.d[1]);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_FPR_COPY_TOP_TO_BOTTOM)
	{
		mov(regRVReg.d[0], regAVReg.d[1]);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_FPR_MULTIPLY_BOTTOM)
	{
		mov(TEMP_FPR_1_VREG.b16, regAVReg.b16);
		fmul(TEMP_FPR_1_VREG.d2, regRVReg.d2, TEMP_FPR_1_VREG.d2);
		mov(regRVReg.d[0], TEMP_FPR_1_VREG.d[0]);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_FPR_MULTIPLY_PAIR)
	{
		fmul(regRVReg.d2, regRVReg.d2, regAVReg.d2);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_FPR_DIVIDE_BOTTOM)
	{
		mov(TEMP_FPR_1_VREG.b16, regAVReg.b16);
		fdiv(TEMP_FPR_1_VREG.d2, regRVReg.d2, TEMP_FPR_1_VREG.d2);
		mov(regRVReg.d[0], TEMP_FPR_1_VREG.d[0]);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_FPR_DIVIDE_PAIR)
	{
		fdiv(regRVReg.d2, regRVReg.d2, regAVReg.d2);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_FPR_ADD_BOTTOM)
	{
		mov(TEMP_FPR_1_VREG.b16, regAVReg.b16);
		fadd(TEMP_FPR_1_VREG.d2, regRVReg.d2, TEMP_FPR_1_VREG.d2);
		mov(regRVReg.d[0], TEMP_FPR_1_VREG.d[0]);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_FPR_ADD_PAIR)
	{
		fadd(regRVReg.d2, regRVReg.d2, regAVReg.d2);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_FPR_SUB_PAIR)
	{
		fsub(regRVReg.d2, regRVReg.d2, regAVReg.d2);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_FPR_SUB_BOTTOM)
	{
		mov(TEMP_FPR_1_VREG.b16, regAVReg.b16);
		fsub(TEMP_FPR_1_VREG.d2, regRVReg.d2, TEMP_FPR_1_VREG.d2);
		mov(regRVReg.d[0], TEMP_FPR_1_VREG.d[0]);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_ASSIGN)
	{
		if (regRId != regAId)
			mov(regRVReg.b16, regAVReg.b16);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_FPR_BOTTOM_FCTIWZ)
	{
		fcvtzs(TEMP_GPR_1_WREG, regADReg);
		mov(regRVReg.d[0], TEMP_GPR_1_XREG);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_FPR_BOTTOM_FRES_TO_BOTTOM_AND_TOP)
	{
		mov(TEMP_GPR_2_XREG, x30);
		mov(TEMP_GPR_1_XREG, (uint64)recompiler_fres);
		mov(ASM_ROUTINE_FPR_VREG.d[0], regAVReg.d[0]);
		blr(TEMP_GPR_1_XREG);
		dup(regRVReg.d2, ASM_ROUTINE_FPR_VREG.d[0]);
		mov(x30, TEMP_GPR_2_XREG);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_FPR_BOTTOM_RECIPROCAL_SQRT)
	{
		mov(TEMP_GPR_2_XREG, x30);
		mov(TEMP_GPR_1_XREG, (uint64)recompiler_frsqrte);
		mov(ASM_ROUTINE_FPR_VREG.d[0], regAVReg.d[0]);
		blr(TEMP_GPR_1_XREG);
		mov(regRVReg.d[0], ASM_ROUTINE_FPR_VREG.d[0]);
		mov(x30, TEMP_GPR_2_XREG);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_FPR_NEGATE_PAIR)
	{
		fneg(regRVReg.d2, regAVReg.d2);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_FPR_ABS_PAIR)
	{
		fabs(regRVReg.d2, regAVReg.d2);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_FPR_FRES_PAIR)
	{
		mov(TEMP_GPR_2_XREG, x30);
		mov(TEMP_GPR_1_XREG, (uint64)recompiler_fres);
		mov(ASM_ROUTINE_FPR_VREG.d[0], regAVReg.d[0]);
		blr(TEMP_GPR_1_XREG);
		mov(regRVReg.d[0], ASM_ROUTINE_FPR_VREG.d[0]);
		mov(ASM_ROUTINE_FPR_VREG.d[0], regAVReg.d[1]);
		blr(TEMP_GPR_1_XREG);
		mov(regRVReg.d[1], ASM_ROUTINE_FPR_VREG.d[0]);
		mov(x30, TEMP_GPR_2_XREG);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_FPR_FRSQRTE_PAIR)
	{
		mov(TEMP_GPR_2_XREG, x30);
		mov(TEMP_GPR_1_XREG, (uint64)recompiler_frsqrte);
		mov(ASM_ROUTINE_FPR_VREG.d[0], regAVReg.d[0]);
		blr(TEMP_GPR_1_XREG);
		mov(regRVReg.d[0], ASM_ROUTINE_FPR_VREG.d[0]);
		mov(ASM_ROUTINE_FPR_VREG.d[0], regAVReg.d[1]);
		blr(TEMP_GPR_1_XREG);
		mov(regRVReg.d[1], ASM_ROUTINE_FPR_VREG.d[0]);
		mov(x30, TEMP_GPR_2_XREG);
	}
	else
	{
		cemu_assert_suspicious();
	}
}

void AArch64GenContext_t::fpr_r_r_r(IMLInstruction* imlInstruction)
{
	auto regR = fpReg<VReg>(imlInstruction->op_fpr_r_r_r.regR.GetRegID());
	auto regA = fpReg<VReg>(imlInstruction->op_fpr_r_r_r.regA.GetRegID());
	auto regB = fpReg<VReg>(imlInstruction->op_fpr_r_r_r.regB.GetRegID());

	if (imlInstruction->operation == PPCREC_IML_OP_FPR_MULTIPLY_BOTTOM)
	{
		fmul(TEMP_FPR_1_VREG.d2, regA.d2, regB.d2);
		mov(regR.d[0], TEMP_FPR_1_VREG.d[0]);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_FPR_ADD_BOTTOM)
	{
		fadd(TEMP_FPR_1_VREG.d2, regA.d2, regB.d2);
		mov(regR.d[0], TEMP_FPR_1_VREG.d[0]);
		mov(regR.d[1], regA.d[1]);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_FPR_SUB_PAIR)
	{
		fsub(regR.d2, regA.d2, regB.d2);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_FPR_SUB_BOTTOM)
	{
		fsub(TEMP_FPR_1_VREG.d2, regA.d2, regB.d2);
		mov(regR.d[0], TEMP_FPR_1_VREG.d[0]);
	}
	else
	{
		cemu_assert_suspicious();
	}
}

/*
 * FPR = op (fprA, fprB, fprC)
 */
void AArch64GenContext_t::fpr_r_r_r_r(IMLInstruction* imlInstruction)
{
	auto regR = fpReg<VReg>(imlInstruction->op_fpr_r_r_r_r.regR.GetRegID());
	auto regA = fpReg<VReg>(imlInstruction->op_fpr_r_r_r_r.regA.GetRegID());
	auto regB = fpReg<VReg>(imlInstruction->op_fpr_r_r_r_r.regB.GetRegID());
	auto regC = fpReg<VReg>(imlInstruction->op_fpr_r_r_r_r.regC.GetRegID());

	if (imlInstruction->operation == PPCREC_IML_OP_FPR_SUM0)
	{
		mov(TEMP_FPR_1_VREG.d[0], regB.d[1]);
		fadd(TEMP_FPR_1_VREG.d2, TEMP_FPR_1_VREG.d2, regA.d2);
		mov(TEMP_FPR_1_VREG.d[1], regC.d[1]);
		mov(regR.b16, TEMP_FPR_1_VREG.b16);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_FPR_SUM1)
	{
		mov(TEMP_FPR_1_VREG.d[1], regA.d[0]);
		fadd(TEMP_FPR_1_VREG.d2, TEMP_FPR_1_VREG.d2, regB.d2);
		mov(TEMP_FPR_1_VREG.d[0], regC.d[0]);
		mov(regR.b16, TEMP_FPR_1_VREG.b16);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_FPR_SELECT_BOTTOM)
	{
		auto regADReg = fpReg<DReg>(imlInstruction->op_fpr_r_r_r_r.regA.GetRegID());
		auto regBDReg = fpReg<DReg>(imlInstruction->op_fpr_r_r_r_r.regB.GetRegID());
		auto regCDReg = fpReg<DReg>(imlInstruction->op_fpr_r_r_r_r.regC.GetRegID());
		fcmp(regADReg, 0.0);
		fcsel(TEMP_FPR_1_DREG, regCDReg, regBDReg, Cond::GE);
		mov(regR.d[0], TEMP_FPR_1_VREG.d[0]);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_FPR_SELECT_PAIR)
	{
		fcmge(TEMP_FPR_1_VREG.d2, regA.d2, 0.0);
		bsl(TEMP_FPR_1_VREG.b16, regC.b16, regB.b16);
		mov(regR.b16, TEMP_FPR_1_VREG.b16);
	}
	else
	{
		cemu_assert_suspicious();
	}
}

void AArch64GenContext_t::fpr_r(IMLInstruction* imlInstruction)
{
	auto regR = fpReg<VReg>(imlInstruction->op_fpr_r.regR.GetRegID());

	if (imlInstruction->operation == PPCREC_IML_OP_FPR_NEGATE_BOTTOM)
	{
		fneg(TEMP_FPR_1_VREG.d2, regR.d2);
		mov(regR.d[0], TEMP_FPR_1_VREG.d[0]);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_FPR_ABS_BOTTOM)
	{
		fabs(TEMP_FPR_1_VREG.d2, regR.d2);
		mov(regR.d[0], TEMP_FPR_1_VREG.d[0]);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_FPR_NEGATIVE_ABS_BOTTOM)
	{
		fabs(TEMP_FPR_1_VREG.d2, regR.d2);
		fneg(TEMP_FPR_1_VREG.d2, TEMP_FPR_1_VREG.d2);
		mov(regR.d[0], TEMP_FPR_1_VREG.d[0]);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_FPR_ROUND_TO_SINGLE_PRECISION_BOTTOM)
	{
		// convert to 32bit single
		fcvtn(TEMP_FPR_1_VREG.s2, regR.d2);
		// convert back to 64bit double
		fcvtl(TEMP_FPR_1_VREG.d2, TEMP_FPR_1_VREG.s2);
		mov(regR.d[0], TEMP_FPR_1_VREG.d[0]);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_FPR_ROUND_TO_SINGLE_PRECISION_PAIR)
	{
		// convert to 32bit singles
		fcvtn(regR.s2, regR.d2);
		// convert back to 64bit doubles
		fcvtl(regR.d2, regR.s2);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_FPR_EXPAND_BOTTOM32_TO_BOTTOM64_AND_TOP64)
	{
		// convert bottom to 64bit double
		fcvtl(regR.d2, regR.s2);
		// copy to top half
		mov(regR.d[1], regR.d[0]);
	}
	else
	{
		cemu_assert_unimplemented();
	}
}

Cond ImlFPCondToArm64Cond(IMLCondition cond)
{
	switch (cond)
	{
	case IMLCondition::UNORDERED_GT:
		return Cond::GT;
	case IMLCondition::UNORDERED_LT:
		return Cond::MI;
	case IMLCondition::UNORDERED_EQ:
		return Cond::EQ;
	case IMLCondition::UNORDERED_U:
		return Cond::VS;
	default:
	{
		cemu_assert_suspicious();
		return Cond::EQ;
	}
	}
}

void AArch64GenContext_t::fpr_compare(IMLInstruction* imlInstruction)
{
	auto regR = gpReg<XReg>(imlInstruction->op_fpr_compare.regR.GetRegID());
	auto regA = fpReg<DReg>(imlInstruction->op_fpr_compare.regA.GetRegID());
	auto regB = fpReg<DReg>(imlInstruction->op_fpr_compare.regB.GetRegID());
	auto cond = ImlFPCondToArm64Cond(imlInstruction->op_fpr_compare.cond);
	fcmp(regA, regB);
	cset(regR, cond);
}

std::unique_ptr<CodeContext> PPCRecompiler_generateAArch64Code(struct PPCRecFunction_t* PPCRecFunction, struct ppcImlGenContext_t* ppcImlGenContext)
{
	auto aarch64GenContext = std::make_unique<AArch64GenContext_t>();
	std::unordered_map<IMLSegment*, Label> labels;
	for (IMLSegment* segIt : ppcImlGenContext->segmentList2)
	{
		if (segIt->nextSegmentBranchTaken != nullptr)
		{
			labels[segIt->nextSegmentBranchTaken] = Label();
		}
	}
	// generate iml instruction code
	bool codeGenerationFailed = false;
	for (IMLSegment* segIt : ppcImlGenContext->segmentList2)
	{
		if (codeGenerationFailed)
			break;
		segIt->x64Offset = aarch64GenContext->getSize();

		if (auto label = labels.find(segIt); label != labels.end())
		{
			aarch64GenContext->L(label->second);
		}

		for (size_t i = 0; i < segIt->imlList.size(); i++)
		{
			IMLInstruction* imlInstruction = segIt->imlList.data() + i;
			if (imlInstruction->type == PPCREC_IML_TYPE_R_NAME)
			{
				aarch64GenContext->r_name(imlInstruction);
			}
			else if (imlInstruction->type == PPCREC_IML_TYPE_NAME_R)
			{
				aarch64GenContext->name_r(imlInstruction);
			}
			else if (imlInstruction->type == PPCREC_IML_TYPE_R_R)
			{
				if (!aarch64GenContext->r_r(imlInstruction))
					codeGenerationFailed = true;
			}
			else if (imlInstruction->type == PPCREC_IML_TYPE_R_S32)
			{
				if (!aarch64GenContext->r_s32(imlInstruction))
					codeGenerationFailed = true;
			}
			else if (imlInstruction->type == PPCREC_IML_TYPE_CONDITIONAL_R_S32)
			{
				if (!aarch64GenContext->conditional_r_s32(imlInstruction))
					codeGenerationFailed = true;
			}
			else if (imlInstruction->type == PPCREC_IML_TYPE_R_R_S32)
			{
				if (!aarch64GenContext->r_r_s32(imlInstruction))
					codeGenerationFailed = true;
			}
			else if (imlInstruction->type == PPCREC_IML_TYPE_R_R_S32_CARRY)
			{
				if (!aarch64GenContext->r_r_s32_carry(imlInstruction))
					codeGenerationFailed = true;
			}
			else if (imlInstruction->type == PPCREC_IML_TYPE_R_R_R)
			{
				if (!aarch64GenContext->r_r_r(imlInstruction))
					codeGenerationFailed = true;
			}
			else if (imlInstruction->type == PPCREC_IML_TYPE_R_R_R_CARRY)
			{
				if (!aarch64GenContext->r_r_r_carry(imlInstruction))
					codeGenerationFailed = true;
			}
			else if (imlInstruction->type == PPCREC_IML_TYPE_COMPARE)
			{
				aarch64GenContext->compare(imlInstruction);
			}
			else if (imlInstruction->type == PPCREC_IML_TYPE_COMPARE_S32)
			{
				aarch64GenContext->compare_s32(imlInstruction);
			}
			else if (imlInstruction->type == PPCREC_IML_TYPE_CONDITIONAL_JUMP)
			{
				if (segIt->nextSegmentBranchTaken == segIt)
					cemu_assert_suspicious();
				aarch64GenContext->cjump(labels, imlInstruction, segIt);
			}
			else if (imlInstruction->type == PPCREC_IML_TYPE_JUMP)
			{
				aarch64GenContext->jump(labels, segIt);
			}
			else if (imlInstruction->type == PPCREC_IML_TYPE_CJUMP_CYCLE_CHECK)
			{
				aarch64GenContext->conditionalJumpCycleCheck(labels, segIt);
			}
			else if (imlInstruction->type == PPCREC_IML_TYPE_MACRO)
			{
				if (!aarch64GenContext->macro(imlInstruction))
					codeGenerationFailed = true;
			}
			else if (imlInstruction->type == PPCREC_IML_TYPE_LOAD)
			{
				if (!aarch64GenContext->load(imlInstruction, false))
					codeGenerationFailed = true;
			}
			else if (imlInstruction->type == PPCREC_IML_TYPE_LOAD_INDEXED)
			{
				if (!aarch64GenContext->load(imlInstruction, true))
					codeGenerationFailed = true;
			}
			else if (imlInstruction->type == PPCREC_IML_TYPE_STORE)
			{
				if (!aarch64GenContext->store(imlInstruction, false))
					codeGenerationFailed = true;
			}
			else if (imlInstruction->type == PPCREC_IML_TYPE_STORE_INDEXED)
			{
				if (!aarch64GenContext->store(imlInstruction, true))
					codeGenerationFailed = true;
			}
			else if (imlInstruction->type == PPCREC_IML_TYPE_ATOMIC_CMP_STORE)
			{
				aarch64GenContext->atomic_cmp_store(imlInstruction);
			}
			else if (imlInstruction->type == PPCREC_IML_TYPE_NO_OP)
			{
			}
			else if (imlInstruction->type == PPCREC_IML_TYPE_FPR_LOAD)
			{
				if (!aarch64GenContext->fpr_load(imlInstruction, false))
					codeGenerationFailed = true;
			}
			else if (imlInstruction->type == PPCREC_IML_TYPE_FPR_LOAD_INDEXED)
			{
				if (!aarch64GenContext->fpr_load(imlInstruction, true))
					codeGenerationFailed = true;
			}
			else if (imlInstruction->type == PPCREC_IML_TYPE_FPR_STORE)
			{
				if (!aarch64GenContext->fpr_store(imlInstruction, false))
					codeGenerationFailed = true;
			}
			else if (imlInstruction->type == PPCREC_IML_TYPE_FPR_STORE_INDEXED)
			{
				if (!aarch64GenContext->fpr_store(imlInstruction, true))
					codeGenerationFailed = true;
			}
			else if (imlInstruction->type == PPCREC_IML_TYPE_FPR_R_R)
			{
				aarch64GenContext->fpr_r_r(imlInstruction);
			}
			else if (imlInstruction->type == PPCREC_IML_TYPE_FPR_R_R_R)
			{
				aarch64GenContext->fpr_r_r_r(imlInstruction);
			}
			else if (imlInstruction->type == PPCREC_IML_TYPE_FPR_R_R_R_R)
			{
				aarch64GenContext->fpr_r_r_r_r(imlInstruction);
			}
			else if (imlInstruction->type == PPCREC_IML_TYPE_FPR_R)
			{
				aarch64GenContext->fpr_r(imlInstruction);
			}
			else if (imlInstruction->type == PPCREC_IML_TYPE_FPR_COMPARE)
			{
				aarch64GenContext->fpr_compare(imlInstruction);
			}
			else
			{
				codeGenerationFailed = true;
				cemu_assert_suspicious();
				cemuLog_log(LogType::Recompiler, "PPCRecompiler_generateX64Code(): Unsupported iml type {:x}", imlInstruction->type);
			}
		}
	}

	// handle failed code generation
	if (codeGenerationFailed)
	{
		return nullptr;
	}
	aarch64GenContext->readyRE();

	// set code
	PPCRecFunction->x86Code = aarch64GenContext->getCode<void*>();
	PPCRecFunction->x86Size = aarch64GenContext->getSize();
	return aarch64GenContext;
}

void AArch64GenContext_t::enterRecompilerCode()
{
	constexpr size_t stackSize = 8 * (30 - 18) /* x18 - x30 */ + 8 * (15 - 8) /*v8.d[0] - v15.d[0]*/ + 8;
	static_assert(stackSize % 16 == 0);
	sub(sp, sp, stackSize);
	mov(x9, sp);

	stp(x19, x20, AdrPostImm(x9, 16));
	stp(x21, x22, AdrPostImm(x9, 16));
	stp(x23, x24, AdrPostImm(x9, 16));
	stp(x25, x26, AdrPostImm(x9, 16));
	stp(x27, x28, AdrPostImm(x9, 16));
	stp(x29, x30, AdrPostImm(x9, 16));
	st4((v8.d - v11.d)[0], AdrPostImm(x9, 32));
	st4((v12.d - v15.d)[0], AdrPostImm(x9, 32));
	mov(HCPU_REG, x1); // call argument 2
	mov(PPC_REC_INSTANCE_REG, (uint64)ppcRecompilerInstanceData);
	mov(MEM_BASE_REG, (uint64)memory_base);

	// branch to recFunc
	blr(x0); // call argument 1

	mov(x9, sp);
	ldp(x19, x20, AdrPostImm(x9, 16));
	ldp(x21, x22, AdrPostImm(x9, 16));
	ldp(x23, x24, AdrPostImm(x9, 16));
	ldp(x25, x26, AdrPostImm(x9, 16));
	ldp(x27, x28, AdrPostImm(x9, 16));
	ldp(x29, x30, AdrPostImm(x9, 16));
	ld4((v8.d - v11.d)[0], AdrPostImm(x9, 32));
	ld4((v12.d - v15.d)[0], AdrPostImm(x9, 32));

	add(sp, sp, stackSize);
	ret();
}

void AArch64GenContext_t::leaveRecompilerCode()
{
	str(LR_WREG, AdrImm(HCPU_REG, offsetof(PPCInterpreter_t, instructionPointer)));
	ret();
}

bool initializedInterfaceFunctions = false;
AArch64GenContext_t enterRecompilerCode_ctx{};

AArch64GenContext_t leaveRecompilerCode_unvisited_ctx{};
AArch64GenContext_t leaveRecompilerCode_visited_ctx{};
void PPCRecompilerAArch64Gen_generateRecompilerInterfaceFunctions()
{
	if (initializedInterfaceFunctions)
		return;
	initializedInterfaceFunctions = true;

	enterRecompilerCode_ctx.enterRecompilerCode();
	enterRecompilerCode_ctx.readyRE();
	PPCRecompiler_enterRecompilerCode = enterRecompilerCode_ctx.getCode<decltype(PPCRecompiler_enterRecompilerCode)>();

	leaveRecompilerCode_unvisited_ctx.leaveRecompilerCode();
	leaveRecompilerCode_unvisited_ctx.readyRE();
	PPCRecompiler_leaveRecompilerCode_unvisited = leaveRecompilerCode_unvisited_ctx.getCode<decltype(PPCRecompiler_leaveRecompilerCode_unvisited)>();

	leaveRecompilerCode_visited_ctx.leaveRecompilerCode();
	leaveRecompilerCode_visited_ctx.readyRE();
	PPCRecompiler_leaveRecompilerCode_visited = leaveRecompilerCode_visited_ctx.getCode<decltype(PPCRecompiler_leaveRecompilerCode_visited)>();

	cemu_assert_debug(PPCRecompiler_leaveRecompilerCode_unvisited != PPCRecompiler_leaveRecompilerCode_visited);
}