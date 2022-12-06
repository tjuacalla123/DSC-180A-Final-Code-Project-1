/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package org.dpppt.android.sdk.util;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import android.util.Base64;




public class SignatureUtil {

	public static PublicKey getPublicKeyFromBase64(String publicKeyBase64)
			throws NoSuchAlgorithmException, InvalidKeySpecException {
		String pubkey = new String(Base64.decode(publicKeyBase64, Base64.NO_WRAP));
		byte[] pubkeyRaw = Base64.decode(pubkey.replaceAll("-+(BEGIN|END) PUBLIC KEY-+", "").trim(), Base64.NO_WRAP);
		return KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(pubkeyRaw));
	}

	public static PublicKey getPublicKeyFromBase64OrThrow(String publicKeyBase64) {
		try {
			return getPublicKeyFromBase64(publicKeyBase64);
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			throw new RuntimeException(e);
		}
	}
}
