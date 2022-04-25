package privruler;

/*-
 * #%L
 * Soot - a J*va Optimization Framework
 * %%
 * Copyright (C) 1997 - 2018 Raja Vallée-Rai and others
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

import soot.BooleanType;
import soot.ByteType;
import soot.CharType;
import soot.DoubleType;
import soot.FloatType;
import soot.IntType;
import soot.LongType;
import soot.RefLikeType;
import soot.ShortType;
import soot.Type;
import soot.Value;
import soot.dexpler.typing.UntypedConstant;
import soot.jimple.DoubleConstant;
import soot.jimple.FloatConstant;
import soot.jimple.IntConstant;
import soot.jimple.LongConstant;
import soot.jimple.NullConstant;

public class MyUntypedConstant extends UntypedConstant {

	/**
	*
	*/
	private static final long serialVersionUID = 4413439694269487822L;
	public final int value;

	private MyUntypedConstant(int value) {
		this.value = value;
	}

	public static MyUntypedConstant v(int value) {
		return new MyUntypedConstant(value);
	}

	@Override
	public boolean equals(Object c) {
		return c instanceof MyUntypedConstant && ((MyUntypedConstant) c).value == this.value;
	}

	/** Returns a hash code for this DoubleConstant object. */
	@Override
	public int hashCode() {
		return (int) (value ^ (value >>> 32));
	}

	public FloatConstant toFloatConstant() {
		return FloatConstant.v(Float.intBitsToFloat((int) value));
	}

	public IntConstant toIntConstant() {
		return IntConstant.v(value);
	}

	public DoubleConstant toDoubleConstant() {
		return DoubleConstant.v(Double.longBitsToDouble(value));
	}

	public LongConstant toLongConstant() {
		return LongConstant.v(value);
	}

	@Override
	public Value defineType(Type t) {
		if (t instanceof FloatType) {
			return this.toFloatConstant();
		} else if (t instanceof IntType || t instanceof CharType || t instanceof BooleanType || t instanceof ByteType
				|| t instanceof ShortType) {
			return this.toIntConstant();
		} else if (t instanceof DoubleType) {
		      return this.toDoubleConstant();
	    } else if (t instanceof LongType) {
	      return this.toLongConstant();
	    } else {
			if (value == 0 && t instanceof RefLikeType) {
				return NullConstant.v();
			}
			// if the value is only used in a if to compare against another integer, then
			// use default type of integer
			if (t == null) {
				return this.toIntConstant();
			}

			throw new RuntimeException("error: expected Float type or Int-like type. Got " + t);
		}   
	}
}
