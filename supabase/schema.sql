-- Create extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Profiles Table
CREATE TABLE profiles (
    id UUID REFERENCES auth.users(id) PRIMARY KEY,
    full_name TEXT NOT NULL,
    email TEXT NOT NULL,
    avatar_url TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- Families Table
CREATE TABLE families (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    name TEXT NOT NULL,
    created_by UUID REFERENCES profiles(id) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- Family Members Table
CREATE TABLE family_members (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    family_id UUID REFERENCES families(id) ON DELETE CASCADE NOT NULL,
    user_id UUID REFERENCES profiles(id) ON DELETE CASCADE NOT NULL,
    role TEXT NOT NULL CHECK (role IN ('ADMIN', 'MEMBER', 'VIEWER')),
    joined_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    UNIQUE(family_id, user_id)
);

-- Categories Table
CREATE TABLE categories (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    name TEXT NOT NULL,
    icon TEXT NOT NULL,
    color TEXT NOT NULL,
    user_id UUID REFERENCES profiles(id),
    family_id UUID REFERENCES families(id) ON DELETE CASCADE,
    is_default BOOLEAN DEFAULT false NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- Transactions Table
CREATE TABLE transactions (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    user_id UUID REFERENCES profiles(id) NOT NULL,
    family_id UUID REFERENCES families(id) ON DELETE CASCADE,
    finance_scope TEXT NOT NULL CHECK (finance_scope IN ('PERSONAL', 'FAMILY')),
    amount NUMERIC(15, 2) NOT NULL,
    transaction_type TEXT NOT NULL CHECK (transaction_type IN ('INCOME', 'EXPENSE')),
    category_id UUID REFERENCES categories(id),
    description TEXT NOT NULL,
    payment_method TEXT NOT NULL,
    upi_id TEXT,
    upi_transaction_id TEXT,
    transaction_date TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    is_deleted BOOLEAN DEFAULT false NOT NULL
);

-- Budgets Table
CREATE TABLE budgets (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    user_id UUID REFERENCES profiles(id) NOT NULL,
    family_id UUID REFERENCES families(id) ON DELETE CASCADE,
    finance_scope TEXT NOT NULL CHECK (finance_scope IN ('PERSONAL', 'FAMILY')),
    name TEXT NOT NULL,
    category_id UUID REFERENCES categories(id),
    amount NUMERIC(15, 2) NOT NULL,
    period_type TEXT NOT NULL,
    start_date TIMESTAMP WITH TIME ZONE,
    end_date TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    is_deleted BOOLEAN DEFAULT false NOT NULL
);

-- Savings Goals Table
CREATE TABLE savings_goals (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    user_id UUID REFERENCES profiles(id) NOT NULL,
    family_id UUID REFERENCES families(id) ON DELETE CASCADE,
    finance_scope TEXT NOT NULL CHECK (finance_scope IN ('PERSONAL', 'FAMILY')),
    name TEXT NOT NULL,
    target_amount NUMERIC(15, 2) NOT NULL,
    current_amount NUMERIC(15, 2) NOT NULL DEFAULT 0,
    target_date TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    is_deleted BOOLEAN DEFAULT false NOT NULL
);

-- Family Invitations Table
CREATE TABLE family_invitations (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    family_id UUID REFERENCES families(id) ON DELETE CASCADE NOT NULL,
    invited_email TEXT NOT NULL,
    invited_by UUID REFERENCES profiles(id) NOT NULL,
    role TEXT NOT NULL CHECK (role IN ('ADMIN', 'MEMBER', 'VIEWER')),
    status TEXT NOT NULL CHECK (status IN ('PENDING', 'ACCEPTED', 'DECLINED', 'EXPIRED')),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- Row Level Security
ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE families ENABLE ROW LEVEL SECURITY;
ALTER TABLE family_members ENABLE ROW LEVEL SECURITY;
ALTER TABLE categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE transactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE budgets ENABLE ROW LEVEL SECURITY;
ALTER TABLE savings_goals ENABLE ROW LEVEL SECURITY;
ALTER TABLE family_invitations ENABLE ROW LEVEL SECURITY;

-- Profiles: Users can see and update their own profile
CREATE POLICY "Users can view their own profile" ON profiles FOR SELECT USING (auth.uid() = id);
CREATE POLICY "Users can update their own profile" ON profiles FOR UPDATE USING (auth.uid() = id);
CREATE POLICY "Users can insert their own profile" ON profiles FOR INSERT WITH CHECK (auth.uid() = id);

-- Profiles: Users can see profiles of their family members
CREATE POLICY "Users can view family member profiles" ON profiles FOR SELECT USING (
    id IN (
        SELECT user_id FROM family_members WHERE family_id IN (
            SELECT family_id FROM family_members WHERE user_id = auth.uid()
        )
    )
);

-- Families: Users can view families they belong to
CREATE POLICY "Users can view families they belong to" ON families FOR SELECT USING (
    id IN (SELECT family_id FROM family_members WHERE user_id = auth.uid())
);
CREATE POLICY "Users can create families" ON families FOR INSERT WITH CHECK (auth.uid() = created_by);

-- Family Members: Users can view members of their families
CREATE POLICY "Users can view family members" ON family_members FOR SELECT USING (
    family_id IN (SELECT family_id FROM family_members WHERE user_id = auth.uid())
);
CREATE POLICY "Users can view their own memberships" ON family_members FOR SELECT USING (
    user_id = auth.uid()
);
CREATE POLICY "Admins can insert family members" ON family_members FOR INSERT WITH CHECK (
    auth.uid() IN (SELECT user_id FROM family_members WHERE family_id = family_members.family_id AND role = 'ADMIN') OR user_id = auth.uid()
);
CREATE POLICY "Admins can update family members" ON family_members FOR UPDATE USING (
    auth.uid() IN (SELECT user_id FROM family_members WHERE family_id = family_members.family_id AND role = 'ADMIN')
);

-- Transactions: Users can view and manage their personal transactions
CREATE POLICY "Users can view personal transactions" ON transactions FOR SELECT USING (
    user_id = auth.uid() AND finance_scope = 'PERSONAL'
);
CREATE POLICY "Users can insert personal transactions" ON transactions FOR INSERT WITH CHECK (
    user_id = auth.uid() AND finance_scope = 'PERSONAL'
);
CREATE POLICY "Users can update personal transactions" ON transactions FOR UPDATE USING (
    user_id = auth.uid() AND finance_scope = 'PERSONAL'
);

-- Transactions: Users can view family transactions if they are a member
CREATE POLICY "Users can view family transactions" ON transactions FOR SELECT USING (
    finance_scope = 'FAMILY' AND family_id IN (SELECT family_id FROM family_members WHERE user_id = auth.uid())
);
CREATE POLICY "Admins and Members can insert family transactions" ON transactions FOR INSERT WITH CHECK (
    finance_scope = 'FAMILY' AND family_id IN (SELECT family_id FROM family_members WHERE user_id = auth.uid() AND role IN ('ADMIN', 'MEMBER'))
);
CREATE POLICY "Admins and Members can update family transactions" ON transactions FOR UPDATE USING (
    finance_scope = 'FAMILY' AND family_id IN (SELECT family_id FROM family_members WHERE user_id = auth.uid() AND role IN ('ADMIN', 'MEMBER'))
);
