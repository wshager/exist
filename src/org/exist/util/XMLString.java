/* eXist Native XML Database
 * Copyright (C) 2000-03,  Wolfgang M. Meier (wolfgang@exist-db.org)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Library General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * $Id$
 */
package org.exist.util;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * Faster string implementation which uses a CharArrayPool to
 * pool the backing char arrays.
 */
public class XMLString implements CharSequence, Comparable {

	public final static int SUPPRESS_NONE = 0;
	public final static int SUPPRESS_LEADING_WS = 0x01;
	public final static int SUPPRESS_TRAILING_WS = 0x02;
	public final static int SUPPRESS_BOTH = SUPPRESS_LEADING_WS | SUPPRESS_TRAILING_WS;

	public final static int DEFAULT_CAPACITY = 16;

	private char[] value_ = null;
	private int start_ = 0;
	private int length_ = 0;

	public XMLString() {
		value_ = CharArrayPool.getCharArray(DEFAULT_CAPACITY);
	}

	public XMLString(int capacity) {
		value_ = CharArrayPool.getCharArray(capacity);
	}

	public XMLString(char[] ch) {
			value_ = CharArrayPool.getCharArray(ch.length);
			System.arraycopy(ch, 0, value_, 0, ch.length);
			length_ = ch.length;
	}

	public XMLString(char[] ch, int start, int length) {
			value_ = CharArrayPool.getCharArray(length);
			System.arraycopy(ch, start, value_, 0, length);
			length_ = length;
	}

	public final XMLString append(String str) {
		append(str.toCharArray());
		return this;
	}

	public final XMLString append(char[] ch) {
		append(ch, 0, ch.length);
		return this;
	}

	public final XMLString append(char[] ch, int offset, int len) {
		ensureCapacity(length_ + len);
		System.arraycopy(ch, offset, value_, length_, len);
		length_ += len;
		return this;
	}

	public final XMLString append(char ch) {
		if(value_.length < length_ + 2)
			ensureCapacity(length_ + 1);
		value_[length_++] = ch;
		return this;
	}
	
	public final void setData(char[] ch, int offset, int len) {
		length_ = 0;
		start_ = 0;
		append(ch, offset, len);
	}

	public final XMLString normalize(int mode) {
		if(length_ == 0)
			return this;
		if ((mode & SUPPRESS_LEADING_WS) != 0) {
			while (length_ > 0 && isWhiteSpace(value_[start_])) {
				--length_;
				if(length_ > 0)
					++start_;
			}
		}
		if ((mode & SUPPRESS_TRAILING_WS) != 0) {
			while (length_ > 0 && isWhiteSpace(value_[start_ + length_ - 1])) {
				--length_;
			}
		}
		return this;
	}

	public final String toString() {
		if (value_ == null)
			return "null";
		return new String(value_, start_, length_);
	}

	public final int length() {
		return length_;
	}

	public final String substring(int start, int count) {
		if (start < 0 || count < 0 || start >= length_ || start + count > length_)
			throw new StringIndexOutOfBoundsException();
		return new String(value_, start_ + start, count);
	}

	public final XMLString delete(int start, int count) {
		System.arraycopy(value_, start + count + start_, value_, start, length_ - (start + count));
		start_ = 0;
		length_ = length_ - count;
		return this;
	}

	public final XMLString insert(int offset, String data) {
		ensureCapacity(length_ + data.length());
		System.arraycopy(value_, offset, value_, offset + data.length(), length_ - offset);
		System.arraycopy(data.toCharArray(), 0, value_, offset, data.length());
		length_ += data.length();
		return this;
	}

	public final XMLString replace(int offset, int count, String data) {
		if (offset < 0 || count < 0 || offset >= length_ || offset + count > length_)
			throw new StringIndexOutOfBoundsException();
		System.arraycopy(data.toCharArray(), 0, value_, start_ + offset, count);
		return this;
	}

	public final char charAt(int pos) {
		return value_[start_ + pos];
	}

	public final void reset() {
		CharArrayPool.releaseCharArray(value_);
		value_ = null;
		start_ = 0;
		length_ = 0;
	}

	private final void ensureCapacity(int capacity) {
		if (value_ == null)
			//value_ = new char[capacity];
			value_ = CharArrayPool.getCharArray(capacity);
		else if (value_.length - start_ < capacity) {
			int newCapacity = (length_ + 1) * 2;
			if (newCapacity < capacity)
				newCapacity = capacity;
			System.out.println("growing " + length_ + " to " + capacity);
			char[] temp = CharArrayPool.getCharArray(newCapacity);
			System.arraycopy(value_, start_, temp, 0, length_);
			CharArrayPool.releaseCharArray(value_);
			value_ = temp;
			start_ = 0;
		}
	}

	private final static boolean isWhiteSpace(char ch) {
		return (ch == 0x20) || (ch == 0x09) || (ch == 0xD) || (ch == 0xA);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#finalize()
	 */
	protected void finalize() throws Throwable {
		CharArrayPool.releaseCharArray(value_);
		value_ = null;
	}

	/* (non-Javadoc)
	 * @see java.lang.CharSequence#subSequence(int, int)
	 */
	public final CharSequence subSequence(int start, int end) {
		return new String(value_, start_ + start, end - start);
	}

	public final XMLString transformToLower() {
		final int end = start_ + length_;
		for (int i = start_; i < end; i++) {
			value_[i] = Character.toLowerCase(value_[i]);
		}
		return this;
	}
	
	public final int UTF8Size() {
		return UTF8.encoded(value_, start_, length_);
	}
	
	public final byte[] UTF8Encode(byte[] b, int offset) {
		return UTF8.encode(value_, start_, length_, b, offset);
	}
	
	public final void toSAX(ContentHandler ch) throws SAXException {
		ch.characters(value_, start_, length_);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public final int compareTo(Object o) {
		CharSequence cs = (CharSequence)o;
		for(int i = 0; i < length_ && i < cs.length(); i++) {
			if(value_[start_ + i] < cs.charAt(i))
				return -1;
			else if(value_[start_ + i] > cs.charAt(i))
				return 1;
		}
		if(length_ < cs.length())
			return -1;
		else if(length_ > cs.length())
			return 1;
		else
			return 0;
	}

}
