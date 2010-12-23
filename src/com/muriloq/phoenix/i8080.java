package com.muriloq.phoenix;

/**
 * Intel 8080 emulator. Based on the Z80 emulator written by 
 * Adam Davidson e Andrew Pollard for Jasper - Java Spectrum Emulator.
 */
public class i8080 extends Object {

    public i8080(double clockFrequencyInMHz) {
        cyclesPerInterrupt = (int) ((clockFrequencyInMHz * 1e6) / 60);
        cycles = -cyclesPerInterrupt;
    }

    public int cyclesPerInterrupt;
    public int cycles;

    private static final int F_C = 0x01; // Carry
    private static final int F_N = 0x02; // Bit 2
    private static final int F_PV = 0x04; // Parity
    private static final int F_3 = 0x08; // Bit 3
    private static final int F_H = 0x10; // Half carry
    private static final int F_5 = 0x20; // Bit 5
    private static final int F_Z = 0x40; // Zero
    private static final int F_S = 0x80; // Signal

    @SuppressWarnings("unused")
    private static final int PF = F_PV;
    @SuppressWarnings("unused")
    private static final int p_ = 0;

    // Parity lookup table
    private static final boolean parity[] = new boolean[256];
    static {
        for (int i = 0; i < 256; i++) {
            boolean p = true;
            for (int j = 0; j < 8; j++) {
                if ((i & (1 << j)) != 0) { // AND is true when we have at least one bit set to 1
                    p = !p; 
                }
            }
            parity[i] = p;
        }
    }

    protected int _A = 0, _HL = 0, _B = 0, _C = 0, _DE = 0;
    protected boolean fS = false, fZ = false, f5 = false, fH = false;
    protected boolean f3 = false, fPV = false, fN = false, fC = false;

    /**
     * Alternate registers: AF is register A + Flags.
     */

    protected int _AF_ = 0, _HL_ = 0, _BC_ = 0, _DE_ = 0;

    /** Stack Pointer e Program Counter */
    protected int _SP = 0, _PC = 0;

    /** Memory */
    public final int mem[] = new int[65536];

    public final int AF() {
        return (A() << 8) | F();
    }

    public final void AF(int word) {
        A(word >> 8);
        F(word & 0xff);
    }

    public final int BC() {
        return (B() << 8) | C();
    }

    public final void BC(int word) {
        B(word >> 8);
        C(word & 0xff);
    }

    public final int DE() {
        return _DE;
    }

    public final void DE(int word) {
        _DE = word;
    }

    public final int HL() {
        return _HL;
    }

    public final void HL(int word) {
        _HL = word;
    }

    public final int PC() {
        return _PC;
    }

    public final void PC(int word) {
        _PC = word;
    }

    public final int SP() {
        return _SP;
    }

    public final void SP(int word) {
        _SP = word;
    }

    public final int F() {
        return (Sset() ? F_S : 0) | (Zset() ? F_Z : 0) | (f5 ? F_5 : 0)
                | (Hset() ? F_H : 0) | (f3 ? F_3 : 0) | (PVset() ? F_PV : 0)
                | (Nset() ? F_N : 0) | (Cset() ? F_C : 0);
    }

    public final void F(int bite) {
        fS = (bite & F_S) != 0;
        fZ = (bite & F_Z) != 0;
        f5 = (bite & F_5) != 0;
        fH = (bite & F_H) != 0;
        f3 = (bite & F_3) != 0;
        fPV = (bite & F_PV) != 0;
        fN = (bite & F_N) != 0;
        fC = (bite & F_C) != 0;
    }

    public final int A() {
        return _A;
    }

    public final void A(int bite) {
        _A = bite;
    }

    public final int B() {
        return _B;
    }

    public final void B(int bite) {
        _B = bite;
    }

    public final int C() {
        return _C;
    }

    public final void C(int bite) {
        _C = bite;
    }

    public final int D() {
        return (_DE >> 8);
    }

    public final void D(int bite) {
        _DE = (bite << 8) | (_DE & 0x00ff);
    }

    public final int E() {
        return (_DE & 0xff);
    }

    public final void E(int bite) {
        _DE = (_DE & 0xff00) | bite;
    }

    public final int H() {
        return (_HL >> 8);
    }

    public final void H(int bite) {
        _HL = (bite << 8) | (_HL & 0x00ff);
    }

    public final int L() {
        return (_HL & 0xff);
    }

    public final void L(int bite) {
        _HL = (_HL & 0xff00) | bite;
    }

    public final void setZ(boolean f) {
        fZ = f;
    }

    public final void setC(boolean f) {
        fC = f;
    }

    public final void setS(boolean f) {
        fS = f;
    }

    public final void setH(boolean f) {
        fH = f;
    }

    public final void setN(boolean f) {
        fN = f;
    }

    public final void setPV(boolean f) {
        fPV = f;
    }

    public final void set3(boolean f) {
        f3 = f;
    }

    public final void set5(boolean f) {
        f5 = f;
    }

    public final boolean Zset() {
        return fZ;
    }

    public final boolean Cset() {
        return fC;
    }

    public final boolean Sset() {
        return fS;
    }

    public final boolean Hset() {
        return fH;
    }

    public final boolean Nset() {
        return fN;
    }

    public final boolean PVset() {
        return fPV;
    }

    public int peekb(int addr) {
        return mem[addr];
    }

    public void pokeb(int addr, int newByte) {
        mem[addr] = newByte;
    }

    public void pokew(int addr, int word) {
        pokeb(addr, word & 0xff);
        addr++;
        pokeb(addr & 0xffff, word >> 8);
    }

    public int peekw(int addr) {
        int t = peekb(addr);
        addr++;
        return t | (peekb(addr & 0xffff) << 8);
    }

    public final void pushw(int word) {
        int sp = ((SP() - 2) & 0xffff);
        SP(sp);
        pokew(sp, word);
    }

    public final int popw() {
        int sp = SP();
        int t = peekb(sp);
        sp++;
        t |= (peekb(sp & 0xffff) << 8);
        SP(++sp & 0xffff);
        return t;
    }

    public final void pushpc() {
        pushw(PC());
    }

    public final void poppc() {
        PC(popw());
    }

    private final int nxtpcb() {
        int pc = PC();
        int t = peekb(pc);
        PC(++pc & 0xffff);
        return t;
    }

    private final int nxtpcw() {
        int pc = PC();
        int t = peekb(pc);
        t |= (peekb(++pc & 0xffff) << 8);
        PC(++pc & 0xffff);
        return t;
    }

    public void reset() {
        PC(0);
        SP(0);
        A(0);
        F(0);
        BC(0);
        DE(0);
        HL(0);
    }

    public void outb(int port, int bite) {
    }

    public int inb(int port) {
        return 0xff;
    }

    public int interrupt() {
        return (0);
    }

    /** i8080 fetch/execute loop */
    public final void execute() {

        switch (nxtpcb()) {

            case 0: /* NOP */
            {
                break;
            }

                /* LXI rr,D16 / DAD rr */

            case 1: /* LXI B,D16 */
            {
                BC(nxtpcw());
                break;
            }
            case 9: /* DAD B */
            {
                HL(add16(HL(), BC()));
                break;
            }
            case 17: /* LXI D,D16 */
            {
                DE(nxtpcw());
                break;
            }
            case 25: /* DAD D */
            {
                HL(add16(HL(), DE()));
                break;
            }
            case 33: /* LXI H,D16 */
            {
                HL(nxtpcw());
                break;
            }
            case 41: /* DAD H */
            {
                int hl = HL();
                HL(add16(hl, hl));
                break;
            }
            case 49: /* LXI SP,D16 */
            {
                SP(nxtpcw());
                break;
            }
            case 57: /* DAD SP */
            {
                HL(add16(HL(), SP()));
                break;
            }

                /* MOV (**),A/A,(**) */
            case 2: /* STAX B */
            {
                pokeb(BC(), A());
                break;
            }
            case 10: /* LDAX B */
            {
                A(peekb(BC()));
                break;
            }
            case 18: /* STAX D */
            {
                pokeb(DE(), A());
                break;
            }
            case 26: /* LDAX D */
            {
                A(peekb(DE()));
                break;
            }
            case 34: /* SHLD Addr */
            {
                pokew(nxtpcw(), HL());
                break;
            }
            case 42: /* LHLD Addr */
            {
                HL(peekw(nxtpcw()));
                break;
            }
            case 50: /* STA Addr */
            {
                pokeb(nxtpcw(), A());
                break;
            }
            case 58: /* LDA Addr */
            {
                A(peekb(nxtpcw()));
                break;
            }

                /* INX/DCX */
            case 3: /* INX B */
            {
                BC(inc16(BC()));
                break;
            }
            case 11: /* DCX B */
            {
                BC(dec16(BC()));
                break;
            }
            case 19: /* INX D */
            {
                DE(inc16(DE()));
                break;
            }
            case 27: /* DCX D */
            {
                DE(dec16(DE()));
                break;
            }
            case 35: /* INX H */
            {
                HL(inc16(HL()));
                break;
            }
            case 43: /* DCX H */
            {
                HL(dec16(HL()));
                break;
            }
            case 51: /* INX SP */
            {
                SP(inc16(SP()));
                break;
            }
            case 59: /* DCX SP */
            {
                SP(dec16(SP()));
                break;
            }

                /* INR * */
            case 4: /* INR B */
            {
                B(inc8(B()));
                break;
            }
            case 12: /* INR C */
            {
                C(inc8(C()));
                break;
            }
            case 20: /* INR D */
            {
                D(inc8(D()));
                break;
            }
            case 28: /* INR E */
            {
                E(inc8(E()));
                break;
            }
            case 36: /* INR H */
            {
                H(inc8(H()));
                break;
            }
            case 44: /* INR L */
            {
                L(inc8(L()));
                break;
            }
            case 52: /* INR M */
            {
                int hl = HL();
                pokeb(hl, inc8(peekb(hl)));
                break;
            }
            case 60: /* INR A */
            {
                A(inc8(A()));
                break;
            }

                /* DCR * */
            case 5: /* DCR B */
            {
                B(dec8(B()));
                break;
            }
            case 13: /* DCR C */
            {
                C(dec8(C()));
                break;
            }
            case 21: /* DCR D */
            {
                D(dec8(D()));
                break;
            }
            case 29: /* DCR E */
            {
                E(dec8(E()));
                break;
            }
            case 37: /* DCR H */
            {
                H(dec8(H()));
                break;
            }
            case 45: /* DCR L */
            {
                L(dec8(L()));
                break;
            }
            case 53: /* DCR M */
            {
                int hl = HL();
                pokeb(hl, dec8(peekb(hl)));
                break;
            }
            case 61: /* DCR A() */
            {
                A(dec8(A()));
                break;
            }

                /* MVI *,D8 */
            case 6: /* MVI B,D8 */
            {
                B(nxtpcb());
                break;
            }
            case 14: /* MVI C,D8 */
            {
                C(nxtpcb());
                break;
            }
            case 22: /* MVI D,D8 */
            {
                D(nxtpcb());
                break;
            }
            case 30: /* MVI E,D8 */
            {
                E(nxtpcb());
                break;
            }
            case 38: /* MVI H,D8 */
            {
                H(nxtpcb());
                break;
            }
            case 46: /* MVI L,D8 */
            {
                L(nxtpcb());
                break;
            }
            case 54: /* MVI M,D8 */
            {
                pokeb(HL(), nxtpcb());

                break;
            }
            case 62: /* MVI A,D8 */
            {
                A(nxtpcb());
                break;
            }

                /* R** */
            case 7: /* RLC */
            {
                rlc();
                break;
            }
            case 15: /* RRC */
            {
                rrc();
                break;
            }
            case 23: /* RAL */
            {
                ral();
                break;
            }
            case 31: /* RAR */
            {
                rar();
                break;
            }
            case 39: /* DAA */
            {
                daa();
                break;
            }
            case 47: /* CMA */
            {
                cma();
                break;
            }
            case 55: /* STC */
            {
                stc();
                break;
            }
            case 63: /* CMC */
            {
                cmc();
                break;
            }

                /* MOV B,* */
            case 64: /* MOV B,B */
            {
                break;
            }
            case 65: /* MOV B,c */
            {
                B(C());
                break;
            }
            case 66: /* MOV B,D */
            {
                B(D());
                break;
            }
            case 67: /* MOV B,E */
            {
                B(E());
                break;
            }
            case 68: /* MOV B,H */
            {
                B(H());
                break;
            }
            case 69: /* MOV B,L */
            {
                B(L());
                break;
            }
            case 70: /* MOV B,M */
            {
                B(peekb(HL()));
                break;
            }
            case 71: /* MOV B,A */
            {
                B(A());
                break;
            }

                /* MOV C,* */
            case 72: /* MOV C,B */
            {
                C(B());
                break;
            }
            case 73: /* MOV C,C */
            {
                break;
            }
            case 74: /* MOV C,D */
            {
                C(D());
                break;
            }
            case 75: /* MOV C,E */
            {
                C(E());
                break;
            }
            case 76: /* MOV C,H */
            {
                C(H());
                break;
            }
            case 77: /* MOV C,L */
            {
                C(L());
                break;
            }
            case 78: /* MOV C,M */
            {
                C(peekb(HL()));
                break;
            }
            case 79: /* MOV C,A */
            {
                C(A());
                break;
            }

                /* MOV D,* */
            case 80: /* MOV D,B */
            {
                D(B());
                break;
            }
            case 81: /* MOV D,C */
            {
                D(C());
                break;
            }
            case 82: /* MOV D,D */
            {
                break;
            }
            case 83: /* MOV D,E */
            {
                D(E());
                break;
            }
            case 84: /* MOV D,H */
            {
                D(H());
                break;
            }
            case 85: /* MOV D,L */
            {
                D(L());
                break;
            }
            case 86: /* MOV D,M */
            {
                D(peekb(HL()));
                break;
            }
            case 87: /* MOV D,A */
            {
                D(A());
                break;
            }

                /* MOV E,* */
            case 88: /* MOV E,B */
            {
                E(B());
                break;
            }
            case 89: /* MOV E,C */
            {
                E(C());
                break;
            }
            case 90: /* MOV E,D */
            {
                E(D());
                break;
            }
            case 91: /* MOV E,E */
            {
                break;
            }
            case 92: /* MOV E,H */
            {
                E(H());
                break;
            }
            case 93: /* MOV E,L */
            {
                E(L());
                break;
            }
            case 94: /* MOV E,M */
            {
                E(peekb(HL()));
                break;
            }
            case 95: /* MOV E,A */
            {
                E(A());
                break;
            }

                /* MOV H,* */
            case 96: /* MOV H,B */
            {
                H(B());
                break;
            }
            case 97: /* MOV H,C */
            {
                H(C());
                break;
            }
            case 98: /* MOV H,D */
            {
                H(D());
                break;
            }
            case 99: /* MOV H,E */
            {
                H(E());
                break;
            }
            case 100: /* MOV H,H */
            {
                break;
            }
            case 101: /* MOV H,L */
            {
                H(L());
                break;
            }
            case 102: /* MOV H,M */
            {
                H(peekb(HL()));
                break;
            }
            case 103: /* MOV H,A */
            {
                H(A());
                break;
            }

                /* MOV L,* */
            case 104: /* MOV L,B */
            {
                L(B());
                break;
            }
            case 105: /* MOV L,C */
            {
                L(C());
                break;
            }
            case 106: /* MOV L,D */
            {
                L(D());
                break;
            }
            case 107: /* MOV L,E */
            {
                L(E());
                break;
            }
            case 108: /* MOV L,H */
            {
                L(H());
                break;
            }
            case 109: /* MOV L,L */
            {
                break;
            }
            case 110: /* MOV L,M */
            {
                L(peekb(HL()));
                break;
            }
            case 111: /* MOV L,A */
            {
                L(A());
                break;
            }

                /* MOV M,* */
            case 112: /* MOV M,B */
            {
                pokeb(HL(), B());
                break;
            }
            case 113: /* MOV M,C */
            {
                pokeb(HL(), C());
                break;
            }
            case 114: /* MOV M,D */
            {
                pokeb(HL(), D());
                break;
            }
            case 115: /* MOV M,E */
            {
                pokeb(HL(), E());
                break;
            }
            case 116: /* MOV M,H */
            {
                pokeb(HL(), H());
                break;
            }
            case 117: /* MOV M,L */
            {
                pokeb(HL(), L());
                break;
            }
            case 118: /* HALT */
            {
                break;
            }
            case 119: /* MOV M,A */
            {
                pokeb(HL(), A());
                break;
            }

                /* MOV A,* */
            case 120: /* MOV A,B */
            {
                A(B());
                break;
            }
            case 121: /* MOV A,C */
            {
                A(C());
                break;
            }
            case 122: /* MOV A,D */
            {
                A(D());
                break;
            }
            case 123: /* MOV A,E */
            {
                A(E());
                break;
            }
            case 124: /* MOV A,H */
            {
                A(H());
                break;
            }
            case 125: /* MOV A,L */
            {
                A(L());
                break;
            }
            case 126: /* MOV A,M */
            {
                A(peekb(HL()));
                break;
            }
            case 127: /* MOV A,A */
            {
                break;
            }

                /* ADD * */
            case 128: /* ADD B */
            {
                add_a(B());
                break;
            }
            case 129: /* ADD C */
            {
                add_a(C());
                break;
            }
            case 130: /* ADD D */
            {
                add_a(D());
                break;
            }
            case 131: /* ADD E */
            {
                add_a(E());
                break;
            }
            case 132: /* ADD H */
            {
                add_a(H());
                break;
            }
            case 133: /* ADD L */
            {
                add_a(L());
                break;
            }
            case 134: /* ADD M */
            {
                add_a(peekb(HL()));
                break;
            }
            case 135: /* ADD A */
            {
                add_a(A());
                break;
            }

                /* ADC * */
            case 136: /* ADC B */
            {
                adc_a(B());
                break;
            }
            case 137: /* ADC C */
            {
                adc_a(C());
                break;
            }
            case 138: /* ADC D */
            {
                adc_a(D());
                break;
            }
            case 139: /* ADC E */
            {
                adc_a(E());
                break;
            }
            case 140: /* ADC H */
            {
                adc_a(H());
                break;
            }
            case 141: /* ADC L */
            {
                adc_a(L());
                break;
            }
            case 142: /* ADC M */
            {
                adc_a(peekb(HL()));
                break;
            }
            case 143: /* ADC A */
            {
                adc_a(A());
                break;
            }

                /* SUB * */
            case 144: /* SUB B */
            {
                sub_a(B());
                break;
            }
            case 145: /* SUB C */
            {
                sub_a(C());
                break;
            }
            case 146: /* SUB D */
            {
                sub_a(D());
                break;
            }
            case 147: /* SUB E */
            {
                sub_a(E());
                break;
            }
            case 148: /* SUB H */
            {
                sub_a(H());
                break;
            }
            case 149: /* SUB L */
            {
                sub_a(L());
                break;
            }
            case 150: /* SUB M */
            {
                sub_a(peekb(HL()));
                break;
            }
            case 151: /* SUB A() */
            {
                sub_a(A());
                break;
            }

                /* SBB A */
            case 152: /* SBB B */
            {
                sbc_a(B());
                break;
            }
            case 153: /* SBB C */
            {
                sbc_a(C());
                break;
            }
            case 154: /* SBB D */
            {
                sbc_a(D());
                break;
            }
            case 155: /* SBB E */
            {
                sbc_a(E());
                break;
            }
            case 156: /* SBB H */
            {
                sbc_a(H());
                break;
            }
            case 157: /* SBB L */
            {
                sbc_a(L());
                break;
            }
            case 158: /* SBB M */
            {
                sbc_a(peekb(HL()));
                break;
            }
            case 159: /* SBB A */
            {
                sbc_a(A());
                break;
            }

                /* ANA * */
            case 160: /* ANA B */
            {
                and_a(B());
                break;
            }
            case 161: /* ANA C */
            {
                and_a(C());
                break;
            }
            case 162: /* ANA D */
            {
                and_a(D());
                break;
            }
            case 163: /* ANA E */
            {
                and_a(E());
                break;
            }
            case 164: /* ANA H */
            {
                and_a(H());
                break;
            }
            case 165: /* ANA L */
            {
                and_a(L());
                break;
            }
            case 166: /* ANA M */
            {
                and_a(peekb(HL()));
                break;
            }
            case 167: /* ANA A */
            {
                and_a(A());
                break;
            }

                /* XRA * */
            case 168: /* XRA B */
            {
                xor_a(B());
                break;
            }
            case 169: /* XRA C */
            {
                xor_a(C());
                break;
            }
            case 170: /* XRA D */
            {
                xor_a(D());
                break;
            }
            case 171: /* XRA E */
            {
                xor_a(E());
                break;
            }
            case 172: /* XRA H */
            {
                xor_a(H());
                break;
            }
            case 173: /* XRA L */
            {
                xor_a(L());
                break;
            }
            case 174: /* XRA M */
            {
                xor_a(peekb(HL()));
                break;
            }
            case 175: /* XRA A() */
            {
                xor_a(A());
                break;
            }

                /* ORA * */
            case 176: /* ORA B */
            {
                or_a(B());
                break;
            }
            case 177: /* ORA C */
            {
                or_a(C());
                break;
            }
            case 178: /* ORA D */
            {
                or_a(D());
                break;
            }
            case 179: /* ORA E */
            {
                or_a(E());
                break;
            }
            case 180: /* ORA H */
            {
                or_a(H());
                break;
            }
            case 181: /* ORA L */
            {
                or_a(L());
                break;
            }
            case 182: /* ORA M */
            {
                or_a(peekb(HL()));
                break;
            }
            case 183: /* ORA A() */
            {
                or_a(A());
                break;
            }

                /* CMP * */
            case 184: /* CMP B */
            {
                cp_a(B());
                break;
            }
            case 185: /* CMP C */
            {
                cp_a(C());
                break;
            }
            case 186: /* CMP D */
            {
                cp_a(D());
                break;
            }
            case 187: /* CMP E */
            {
                cp_a(E());
                break;
            }
            case 188: /* CMP H */
            {
                cp_a(H());
                break;
            }
            case 189: /* CMP L */
            {
                cp_a(L());
                break;
            }
            case 190: /* CMP M */
            {
                cp_a(peekb(HL()));
                break;
            }
            case 191: /* CMP A() */
            {
                cp_a(A());
                break;
            }

                /* Rcc */
            case 192: /* RNZ */
            {
                if (!Zset()) {
                    poppc();

                } else {

                }
                break;
            }
            case 200: /* RZ */
            {
                if (Zset()) {
                    poppc();

                } else {

                }
                break;
            }
            case 208: /* RNC */
            {
                if (!Cset()) {
                    poppc();

                } else {

                }
                break;
            }
            case 216: /* RC */
            {
                if (Cset()) {
                    poppc();

                } else {

                }
                break;
            }
            case 224: /* RPO */
            {
                if (!PVset()) {
                    poppc();

                } else {

                }
                break;
            }
            case 232: /* RPE */
            {
                if (PVset()) {
                    poppc();

                } else {

                }
                break;
            }
            case 240: /* RP */
            {
                if (!Sset()) {
                    poppc();

                } else {

                }
                break;
            }
            case 248: /* RM */
            {
                if (Sset()) {
                    poppc();

                } else {

                }
                break;
            }

                /* POP * */
            case 193: /* POP B */
            {
                BC(popw());
                break;
            }
            case 201: /* RET */
            {
                poppc();
                break;
            }
            case 209: /* POP D */
            {
                DE(popw());
                break;
            }
            case 225: /* POP H */
            {
                HL(popw());
                break;
            }
            case 233: /* PCHL */
            {
                PC(HL());
                break;
            }
            case 241: /* POP PSW */
            {
                AF(popw());
                break;
            }
            case 249: /* SPHL */
            {
                SP(HL());
                break;
            }

                /* Jcc Addr */
            case 194: /* JNZ Addr */
            {
                if (!Zset()) {
                    PC(nxtpcw());
                } else {
                    PC((PC() + 2) & 0xffff);
                }

                break;
            }
            case 202: /* JZ Addr */
            {
                if (Zset()) {
                    PC(nxtpcw());
                } else {
                    PC((PC() + 2) & 0xffff);
                }

                break;
            }
            case 210: /* JNC Addr */
            {
                if (!Cset()) {
                    PC(nxtpcw());
                } else {
                    PC((PC() + 2) & 0xffff);
                }

                break;
            }
            case 218: /* JC Addr */
            {
                if (Cset()) {
                    PC(nxtpcw());
                } else {
                    PC((PC() + 2) & 0xffff);
                }

                break;
            }
            case 226: /* JPO Addr */
            {
                if (!PVset()) {
                    PC(nxtpcw());
                } else {
                    PC((PC() + 2) & 0xffff);
                }

                break;
            }
            case 234: /* JPE Addr */
            {
                if (PVset()) {
                    PC(nxtpcw());
                } else {
                    PC((PC() + 2) & 0xffff);
                }

                break;
            }
            case 242: /* JP Addr */
            {
                if (!Sset()) {
                    PC(nxtpcw());
                } else {
                    PC((PC() + 2) & 0xffff);
                }

                break;
            }
            case 250: /* JM Addr */
            {
                if (Sset()) {
                    PC(nxtpcw());
                } else {
                    PC((PC() + 2) & 0xffff);
                }

                break;
            }

                /* Various */
            case 195: /* JMP Addr */
            {
                PC(peekw(PC()));
                break;
            }

            case 211: /* OUT D8 */
            {
                outb(nxtpcb(), A());

                break;
            }
            case 219: /* IN D8 */
            {
                A(inb((A() << 8) | nxtpcb()));

                break;
            }
            case 227: /* XTHL */
            {
                int t = HL();
                int sp = SP();
                HL(peekw(sp));
                pokew(sp, t);

                break;
            }
            case 235: /* XCHG */
            {
                int t = HL();
                HL(DE());
                DE(t);

                break;
            }
            case 243: /* DI */
            {
                break;
            }
            case 251: /* EI */
            {
                break;
            }

                /* Ccc Addr */
            case 196: /* CNZ Addr */
            {
                if (!Zset()) {
                    int t = nxtpcw();
                    pushpc();
                    PC(t);

                } else {
                    PC((PC() + 2) & 0xffff);

                }
                break;
            }
            case 204: /* CZ Addr */
            {
                if (Zset()) {
                    int t = nxtpcw();
                    pushpc();
                    PC(t);

                } else {
                    PC((PC() + 2) & 0xffff);

                }
                break;
            }
            case 212: /* CNC Addr */
            {
                if (!Cset()) {
                    int t = nxtpcw();
                    pushpc();
                    PC(t);

                } else {
                    PC((PC() + 2) & 0xffff);

                }
                break;
            }
            case 220: /* CC Addr */
            {
                if (Cset()) {
                    int t = nxtpcw();
                    pushpc();
                    PC(t);

                } else {
                    PC((PC() + 2) & 0xffff);

                }
                break;
            }
            case 228: /* CPO Addr */
            {
                if (!PVset()) {
                    int t = nxtpcw();
                    pushpc();
                    PC(t);

                } else {
                    PC((PC() + 2) & 0xffff);

                }
                break;
            }
            case 236: /* CPE Addr */
            {
                if (PVset()) {
                    int t = nxtpcw();
                    pushpc();
                    PC(t);

                } else {
                    PC((PC() + 2) & 0xffff);

                }
                break;
            }
            case 244: /* CP Addr */
            {
                if (!Sset()) {
                    int t = nxtpcw();
                    pushpc();
                    PC(t);

                } else {
                    PC((PC() + 2) & 0xffff);

                }
                break;
            }
            case 252: /* CM Addr */
            {
                if (Sset()) {
                    int t = nxtpcw();
                    pushpc();
                    PC(t);

                } else {
                    PC((PC() + 2) & 0xffff);

                }
                break;
            }

                /* PUSH * */
            case 197: /* PUSH B */
            {
                pushw(BC());
                break;
            }
            case 205: /* CALL Addr */
            {
                int t = nxtpcw();
                pushpc();
                PC(t);

                break;
            }
            case 213: /* PUSH D */
            {
                pushw(DE());
                break;
            }
            case 229: /* PUSH H */
            {
                pushw(HL());
                break;
            }
            case 245: /* PUSH PSW */
            {
                pushw(AF());
                break;
            }

                /* op N */
            case 198: /* ADD N */
            {
                add_a(nxtpcb());
                break;
            }
            case 206: /* ADC N */
            {
                adc_a(nxtpcb());
                break;
            }
            case 214: /* SUB N */
            {
                sub_a(nxtpcb());
                break;
            }
            case 222: /* SBB N */
            {
                sbc_a(nxtpcb());
                break;
            }
            case 230: /* AND N */
            {
                and_a(nxtpcb());
                break;
            }
            case 238: /* XRI N */
            {
                xor_a(nxtpcb());
                break;
            }
            case 246: /* ORA N */
            {
                or_a(nxtpcb());
                break;
            }
            case 254: /* CPI N */
            {
                cp_a(nxtpcb());
                break;
            }

                /* RST n */
            case 199: /* RST 0 */
            {
                pushpc();
                PC(0);
                break;
            }
            case 207: /* RST 8 */
            {
                pushpc();
                PC(8);
                break;
            }
            case 215: /* RST 16 */
            {
                pushpc();
                PC(16);
                break;
            }
            case 223: /* RST 24 */
            {
                pushpc();
                PC(24);
                break;
            }
            case 231: /* RST 32 */
            {
                pushpc();
                PC(32);
                break;
            }
            case 239: /* RST 40 */
            {
                pushpc();
                PC(40);
                break;
            }
            case 247: /* RST 48 */
            {
                pushpc();
                PC(48);
                break;
            }
            case 255: /* RST 56 */
            {
                pushpc();
                PC(56);
                break;
            }

        }

    }

    /** Add with carry - alters all flags */
    private final void adc_a(int b) {
        int a = A();
        int c = Cset() ? 1 : 0;
        int wans = a + b + c;
        int ans = wans & 0xff;

        setS((ans & F_S) != 0);
        set3((ans & F_3) != 0);
        set5((ans & F_5) != 0);
        setZ((ans) == 0);
        setC((wans & 0x100) != 0);
        setPV(((a ^ ~b) & (a ^ ans) & 0x80) != 0);
        setH((((a & 0x0f) + (b & 0x0f) + c) & F_H) != 0);
        setN(false);

        A(ans);
    }

    /** Add - alters all flags */
    private final void add_a(int b) {
        int a = A();
        int wans = a + b;
        int ans = wans & 0xff;

        setS((ans & F_S) != 0);
        set3((ans & F_3) != 0);
        set5((ans & F_5) != 0);
        setZ((ans) == 0);
        setC((wans & 0x100) != 0);
        setPV(((a ^ ~b) & (a ^ ans) & 0x80) != 0);
        setH((((a & 0x0f) + (b & 0x0f)) & F_H) != 0);
        setN(false);

        A(ans);
    }

    /** Subtract with carry - alters all flags */
    private final void sbc_a(int b) {
        int a = A();
        int c = Cset() ? 1 : 0;
        int wans = a - b - c;
        int ans = wans & 0xff;

        setS((ans & F_S) != 0);
        set3((ans & F_3) != 0);
        set5((ans & F_5) != 0);
        setZ((ans) == 0);
        setC((wans & 0x100) != 0);
        setPV(((a ^ b) & (a ^ ans) & 0x80) != 0);
        setH((((a & 0x0f) - (b & 0x0f) - c) & F_H) != 0);
        setN(true);

        A(ans);
    }

    /** Subtract - alters all flags */
    private final void sub_a(int b) {
        int a = A();
        int wans = a - b;
        int ans = wans & 0xff;

        setS((ans & F_S) != 0);
        set3((ans & F_3) != 0);
        set5((ans & F_5) != 0);
        setZ((ans) == 0);
        setC((wans & 0x100) != 0);
        setPV(((a ^ b) & (a ^ ans) & 0x80) != 0);
        setH((((a & 0x0f) - (b & 0x0f)) & F_H) != 0);
        setN(true);

        A(ans);
    }

    /** Rotate Left - alters H N C 3 5 flags */
    private final void rlc() {
        int ans = A();
        boolean c = (ans & 0x80) != 0;

        if (c) {
            ans = (ans << 1) | 0x01;
        } else {
            ans <<= 1;
        }
        ans &= 0xff;

        set3((ans & F_3) != 0);
        set5((ans & F_5) != 0);
        setN(false);
        setH(false);
        setC(c);

        A(ans);
    }

    /** Rotate Right - alters H N C 3 5 flags */
    private final void rrc() {
        int ans = A();
        boolean c = (ans & 0x01) != 0;

        if (c) {
            ans = (ans >> 1) | 0x80;
        } else {
            ans >>= 1;
        }

        set3((ans & F_3) != 0);
        set5((ans & F_5) != 0);
        setN(false);
        setH(false);
        setC(c);

        A(ans);
    }

    /** Rotate Left through Carry - alters H N C 3 5 flags */
    private final void ral() {
        int ans = A();
        boolean c = (ans & 0x80) != 0;

        if (Cset()) {
            ans = (ans << 1) | 0x01;
        } else {
            ans <<= 1;
        }

        ans &= 0xff;

        set3((ans & F_3) != 0);
        set5((ans & F_5) != 0);
        setN(false);
        setH(false);
        setC(c);

        A(ans);
    }

    /** Rotate Right through Carry - alters H N C 3 5 flags */
    private final void rar() {
        int ans = A();
        boolean c = (ans & 0x01) != 0;

        if (Cset()) {
            ans = (ans >> 1) | 0x80;
        } else {
            ans >>= 1;
        }

        set3((ans & F_3) != 0);
        set5((ans & F_5) != 0);
        setN(false);
        setH(false);
        setC(c);

        A(ans);
    }

    /** Compare - alters all flags */
    private final void cp_a(int b) {
        int a = A();
        int wans = a - b;
        int ans = wans & 0xff;

        setS((ans & F_S) != 0);
        set3((b & F_3) != 0);
        set5((b & F_5) != 0);
        setN(true);
        setZ(ans == 0);
        setC((wans & 0x100) != 0);
        setH((((a & 0x0f) - (b & 0x0f)) & F_H) != 0);
        setPV(((a ^ b) & (a ^ ans) & 0x80) != 0);
    }

    /** Bitwise and - alters all flags */
    private final void and_a(int b) {
        int ans = A() & b;

        setS((ans & F_S) != 0);
        set3((ans & F_3) != 0);
        set5((ans & F_5) != 0);
        setH(true);
        setPV(parity[ans]);
        setZ(ans == 0);
        setN(false);
        setC(false);

        A(ans);
    }

    /** Bitwise or - alters all flags */
    private final void or_a(int b) {
        int ans = A() | b;

        setS((ans & F_S) != 0);
        set3((ans & F_3) != 0);
        set5((ans & F_5) != 0);
        setH(false);
        setPV(parity[ans]);
        setZ(ans == 0);
        setN(false);
        setC(false);

        A(ans);
    }

    /** Bitwise exclusive or - alters all flags */
    private final void xor_a(int b) {
        int ans = (A() ^ b) & 0xff;

        setS((ans & F_S) != 0);
        set3((ans & F_3) != 0);
        set5((ans & F_5) != 0);
        setH(false);
        setPV(parity[ans]);
        setZ(ans == 0);
        setN(false);
        setC(false);

        A(ans);
    }

    /** One's complement - alters N H 3 5 flags */
    private final void cma() {
        int ans = A() ^ 0xff;

        set3((ans & F_3) != 0);
        set5((ans & F_5) != 0);
        setH(true);
        setN(true);

        A(ans);
    }

    /** Decimal Adjust Accumulator - alters all flags */
    private final void daa() {
        int ans = A();
        int incr = 0;
        boolean carry = Cset();

        if ((Hset()) || ((ans & 0x0f) > 0x09)) {
            incr |= 0x06;
        }
        if (carry || (ans > 0x9f) || ((ans > 0x8f) && ((ans & 0x0f) > 0x09))) {
            incr |= 0x60;
        }
        if (ans > 0x99) {
            carry = true;
        }
        if (Nset()) {
            sub_a(incr);
        } else {
            add_a(incr);
        }

        ans = A();

        setC(carry);
        setPV(parity[ans]);
    }

    /** Set carry flag - alters N H 3 5 C flags */
    private final void stc() {
        int ans = A();

        set3((ans & F_3) != 0);
        set5((ans & F_5) != 0);
        setN(false);
        setH(false);
        setC(true);
    }

    /** Complement carry flag - alters N 3 5 C flags */
    private final void cmc() {
        int ans = A();

        set3((ans & F_3) != 0);
        set5((ans & F_5) != 0);
        setN(false);
        setC(Cset() ? false : true);
    }

    /** Rotate left - alters all flags */
    @SuppressWarnings("unused")
    private final int rlc(int ans) {
        boolean c = (ans & 0x80) != 0;

        if (c) {
            ans = (ans << 1) | 0x01;
        } else {
            ans <<= 1;
        }
        ans &= 0xff;

        setS((ans & F_S) != 0);
        set3((ans & F_3) != 0);
        set5((ans & F_5) != 0);
        setZ((ans) == 0);
        setPV(parity[ans]);
        setH(false);
        setN(false);
        setC(c);

        return (ans);
    }

    /** Rotate right - alters all flags */
    @SuppressWarnings("unused")
    private final int rrc(int ans) {
        boolean c = (ans & 0x01) != 0;

        if (c) {
            ans = (ans >> 1) | 0x80;
        } else {
            ans >>= 1;
        }

        setS((ans & F_S) != 0);
        set3((ans & F_3) != 0);
        set5((ans & F_5) != 0);
        setZ((ans) == 0);
        setPV(parity[ans]);
        setH(false);
        setN(false);
        setC(c);

        return (ans);
    }

    /** Rotate left through carry - alters all flags */
    @SuppressWarnings("unused")
    private final int rl(int ans) {
        boolean c = (ans & 0x80) != 0;

        if (Cset()) {
            ans = (ans << 1) | 0x01;
        } else {
            ans <<= 1;
        }
        ans &= 0xff;

        setS((ans & F_S) != 0);
        set3((ans & F_3) != 0);
        set5((ans & F_5) != 0);
        setZ((ans) == 0);
        setPV(parity[ans]);
        setH(false);
        setN(false);
        setC(c);

        return (ans);
    }

    /** Rotate right through carry - alters all flags */
    @SuppressWarnings("unused")
    private final int rr(int ans) {
        boolean c = (ans & 0x01) != 0;

        if (Cset()) {
            ans = (ans >> 1) | 0x80;
        } else {
            ans >>= 1;
        }

        setS((ans & F_S) != 0);
        set3((ans & F_3) != 0);
        set5((ans & F_5) != 0);
        setZ((ans) == 0);
        setPV(parity[ans]);
        setH(false);
        setN(false);
        setC(c);

        return (ans);
    }

    /** Decrement - alters all but C flag */
    private final int dec8(int ans) {
        boolean pv = (ans == 0x80);
        boolean h = (((ans & 0x0f) - 1) & F_H) != 0;
        ans = (ans - 1) & 0xff;

        setS((ans & F_S) != 0);
        set3((ans & F_3) != 0);
        set5((ans & F_5) != 0);
        setZ((ans) == 0);
        setPV(pv);
        setH(h);
        setN(true);

        return (ans);
    }

    /** Increment - alters all but C flag */
    private final int inc8(int ans) {
        boolean pv = (ans == 0x7f);
        boolean h = (((ans & 0x0f) + 1) & F_H) != 0;
        ans = (ans + 1) & 0xff;

        setS((ans & F_S) != 0);
        set3((ans & F_3) != 0);
        set5((ans & F_5) != 0);
        setZ((ans) == 0);
        setPV(pv);
        setH(h);
        setN(false);

        return (ans);
    }

    /** Add with carry */
    @SuppressWarnings("unused")
    private final int adc16(int a, int b) {
        int c = Cset() ? 1 : 0;
        int lans = a + b + c;
        int ans = lans & 0xffff;

        setS((ans & (F_S << 8)) != 0);
        set3((ans & (F_3 << 8)) != 0);
        set5((ans & (F_5 << 8)) != 0);
        setZ((ans) == 0);
        setC((lans & 0x10000) != 0);
        setPV(((a ^ ~b) & (a ^ ans) & 0x8000) != 0);
        setH((((a & 0x0fff) + (b & 0x0fff) + c) & 0x1000) != 0);
        setN(false);

        return (ans);
    }

    /** Add */
    private final int add16(int a, int b) {
        int lans = a + b;
        int ans = lans & 0xffff;

        set3((ans & (F_3 << 8)) != 0);
        set5((ans & (F_5 << 8)) != 0);
        setC((lans & 0x10000) != 0);
        setH((((a & 0x0fff) + (b & 0x0fff)) & 0x1000) != 0);
        setN(false);

        return (ans);
    }

    /** Add with carry */
    @SuppressWarnings("unused")
    private final int sbc16(int a, int b) {
        int c = Cset() ? 1 : 0;
        int lans = a - b - c;
        int ans = lans & 0xffff;

        setS((ans & (F_S << 8)) != 0);
        set3((ans & (F_3 << 8)) != 0);
        set5((ans & (F_5 << 8)) != 0);
        setZ((ans) == 0);
        setC((lans & 0x10000) != 0);
        setPV(((a ^ b) & (a ^ ans) & 0x8000) != 0);
        setH((((a & 0x0fff) - (b & 0x0fff) - c) & 0x1000) != 0);
        setN(true);

        return (ans);
    }

    /** Quick Increment : no flags */
    private static final int inc16(int a) {
        return (a + 1) & 0xffff;
    }

    @SuppressWarnings("unused")
    private static final int qinc8(int a) {
        return (a + 1) & 0xff;
    }

    /** Quick Decrement : no flags */
    private static final int dec16(int a) {
        return (a - 1) & 0xffff;
    }

    @SuppressWarnings("unused")
    private static final int qdec8(int a) {
        return (a - 1) & 0xff;
    }

    /** Bit toggling */
    @SuppressWarnings("unused")
    private static final int res(int bit, int val) {
        return val & ~bit;
    }

    @SuppressWarnings("unused")
    private static final int set(int bit, int val) {
        return val | bit;
    }
}
